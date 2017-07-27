package com.lightbend.modelserver

/**
  * Created by boris on 6/26/17.
  */

import com.lightbend.configuration.kafka.ApplicationKafkaParameters
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.modelserver.kafka.KafkaSupport
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import com.lightbend.model.scala.{DataWithModel, Model}
import com.lightbend.modelserver.support.scala.ModelToServe
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming._
import org.apache.spark.SparkConf


object SparkModelServer {

  private val factories = Map(ModelDescriptor.ModelType.PMML.value -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.value -> TensorFlowModel)

  def main(args: Array[String]): Unit = {
    // Create context
    val sparkConf = new SparkConf()
      .setAppName("SparkModelServer")
      .setMaster("local")
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    sparkConf.set("spark.kryo.registrator", "com.lightbend.modelserver.ModelRegistrator")
//    sparkConf.set("spark.kryo.registrator", "com.lightbend.modelServer.model.TensorFlowModelRegistrator")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    ssc.checkpoint("./cpt")

    // Initial state RDD for current models
    val modelsRDD = ssc.sparkContext.emptyRDD[(String, Model)]

    // Create models kafka stream
    val kafkaParams = KafkaSupport.getKafkaConsumerConfig(ApplicationKafkaParameters.LOCAL_KAFKA_BROKER)
    val modelsStream = KafkaUtils.createDirectStream[Array[Byte], Array[Byte]](ssc,PreferConsistent,
      Subscribe[Array[Byte], Array[Byte]](Set(ApplicationKafkaParameters.MODELS_TOPIC),kafkaParams))
    // Create data kafka stream
    val dataStream = KafkaUtils.createDirectStream[Array[Byte], Array[Byte]](ssc, PreferConsistent,
      Subscribe[Array[Byte], Array[Byte]](Set(ApplicationKafkaParameters.DATA_TOPIC),kafkaParams))
    // Convert streams
    val data = dataStream.map(r =>
      DataRecord.fromByteArray(r.value())).
      filter(_.isSuccess).map(d => DataWithModel(None, Some(d.get)))
    val models = modelsStream.map(r => ModelToServe.fromByteArray(r.value())).
      filter(_.isSuccess).map(m => DataWithModel(Some(m.get), None))
    // Combine streams
    val unionStream = ssc.union(Seq(data, models)).map(r => (r.getDataType, r))

    // score model using mapWithState, where state contains current model
    val mappingFunc = (dataType: String, dataWithModel: Option[DataWithModel], state: State[Model]) => {
      val currentModel = state.getOption().getOrElse(null.asInstanceOf[Model])
      dataWithModel match {
        case Some(value) =>
          if (value.isModel) {
            // Proces model
            if (currentModel != null) currentModel.cleanup()
            val model = factories.get(value.getModel.modelType.value) match {
              case Some(factory) => factory.create(value.getModel)
              case _ => None
            }
            model match {
              case Some(m) => state.update(m)
              case _ =>
            }
            None
          }
          else {
            // process data
            if (currentModel != null)
              Some(currentModel.score(value.getData.asInstanceOf[AnyVal]).asInstanceOf[Double])
            else
              None
          }
        case _ => None
      }
    }
    // Define StateSpec<KeyType,ValueType,StateType,MappedType> - types are derived from function
    val resultDstream = unionStream.mapWithState(StateSpec.function(mappingFunc).initialState(modelsRDD))
    resultDstream.print()
    // Execute
    ssc.start()
    ssc.awaitTermination()
  }
}