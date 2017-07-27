package com.lightbend.modelserver.keyed

import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.model.scala.Model
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import com.lightbend.modelserver.typeschema.ModelTypeSerializer
import com.lightbend.modelserver.support.scala.{ModelToServe, ModelToServeStats}
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueState, ValueStateDescriptor}
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.{CheckpointedFunction, CheckpointedRestoring}
import org.apache.flink.streaming.api.functions.co.CoProcessFunction
import org.apache.flink.util.Collector

/**
  * Created by boris on 5/8/17.
  *
  * Main class processing data using models
  *
  * see http://dataartisans.github.io/flink-training/exercises/eventTimeJoin.html for details
  */

object DataProcessorKeyed {
  def apply() = new DataProcessorKeyed
  private val factories = Map(ModelDescriptor.ModelType.PMML -> PMMLModel,
              ModelDescriptor.ModelType.TENSORFLOW -> TensorFlowModel)
}

class DataProcessorKeyed extends CoProcessFunction[WineRecord, ModelToServe, Double]
        with CheckpointedFunction with CheckpointedRestoring[List[Option[Model]]] {

  // The managed keyed state see https://ci.apache.org/projects/flink/flink-docs-release-1.3/dev/stream/state.html
  var modelState: ValueState[ModelToServeStats] = _
  var newModelState: ValueState[ModelToServeStats] = _

  var currentModel : Option[Model] = None
  var newModel : Option[Model] = None

  @transient private var checkpointedState: ListState[Option[Model]] = null


  override def open(parameters: Configuration): Unit = {
    val modelDesc = new ValueStateDescriptor[ModelToServeStats](
      "currentModel",   // state name
      createTypeInformation[ModelToServeStats]) // type information
    modelDesc.setQueryable("currentModel")

    modelState = getRuntimeContext.getState(modelDesc)
    val newModelDesc = new ValueStateDescriptor[ModelToServeStats](
      "newModel",         // state name
      createTypeInformation[ModelToServeStats])  // type information
    newModelState = getRuntimeContext.getState(newModelDesc)
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointedState.clear()
    checkpointedState.add(currentModel)
    checkpointedState.add(newModel)
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val descriptor = new ListStateDescriptor[Option[Model]] (
      "modelState",
      new ModelTypeSerializer)

    checkpointedState = context.getOperatorStateStore.getListState (descriptor)

    if (context.isRestored) {
      val iterator = checkpointedState.get().iterator()
      currentModel = iterator.next()
      newModel = iterator.next()
    }
  }

  override def restoreState(state: List[Option[Model]]): Unit = {
    currentModel = state(0)
    newModel = state(1)
  }

  override def processElement2(model: ModelToServe, ctx: CoProcessFunction[WineRecord, ModelToServe, Double]#Context, out: Collector[Double]): Unit = {

    import DataProcessorKeyed._

    println(s"New model - $model")
    newModelState.update(new ModelToServeStats(model))
    newModel = factories.get(model.modelType) match {
      case Some(factory) => factory.create (model)
      case _ => None
    }
  }

  override def processElement1(record: WineRecord, ctx: CoProcessFunction[WineRecord, ModelToServe, Double]#Context, out: Collector[Double]): Unit = {

    // See if we have update for the model
    newModel match {
      case Some(model) => {
        // Clean up current model
        currentModel match {
          case Some(m) => m.cleanup()
          case _ =>
        }
        // Update model
        currentModel = Some(model)
        modelState.update(newModelState.value())
        newModel = None
      }
      case _ =>
    }
    currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(record.asInstanceOf[AnyVal]).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        modelState.update(modelState.value().incrementUsage(duration))
        println(s"Calculated quality - $quality calculated in $duration ms")
      }
      case _ => println("No model available - skipping")
    }
  }
}
