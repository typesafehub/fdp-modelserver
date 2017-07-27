package com.lightbend.modelserver.partitioned

/**
  * Created by boris on 5/14/17.
  *
  * Main class processing data using models
  *
  */
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.winerecord.WineRecord
import com.lightbend.model.scala.Model
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import com.lightbend.modelserver.typeschema.ModelTypeSerializer
import com.lightbend.modelserver.support.scala.ModelToServe
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.{CheckpointedFunction, CheckpointedRestoring}
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction
import org.apache.flink.util.Collector

object DataProcessorMap{
  def apply() : DataProcessorMap = new DataProcessorMap()

  private val factories = Map(ModelDescriptor.ModelType.PMML -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW -> TensorFlowModel)
}

class DataProcessorMap extends RichCoFlatMapFunction[WineRecord, ModelToServe, Double]
        with CheckpointedFunction with CheckpointedRestoring[List[Option[Model]]] {

  var currentModel : Option[Model] = None
  var newModel : Option[Model] = None
  @transient private var checkpointedState: ListState[Option[Model]] = null

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

  override def flatMap2(model: ModelToServe, out: Collector[Double]): Unit = {

    import DataProcessorMap._

    println(s"New model - $model")
    newModel = factories.get(model.modelType) match{
      case Some(factory) => factory.create(model)
      case _ => None
    }
  }

  override def flatMap1(record: WineRecord, out: Collector[Double]): Unit = {
    // See if we need to update
    newModel match {
      case Some(model) => {
        // close current model first
        currentModel match {
          case Some(m) => m.cleanup();
          case _ =>
        }
        // Update model
        currentModel = Some(model)
        newModel = None
      }
      case _ =>
    }
    currentModel match {
      case Some(model) => {
        val start = System.currentTimeMillis()
        val quality = model.score(record.asInstanceOf[AnyVal]).asInstanceOf[Double]
        val duration = System.currentTimeMillis() - start
        println(s"Subtask ${this.getRuntimeContext.getIndexOfThisSubtask} calculated quality - $quality calculated in $duration ms")
      }
      case _ => println("No model available - skipping")
    }
  }
}