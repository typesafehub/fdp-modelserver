package com.lightbend.modelserver

/**
  * Created by boris on 6/2/17.
  */

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.scala.Model
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import org.apache.spark.serializer.KryoRegistrator


class ModelSerializerKryo extends Serializer[Model]{
  
  super.setAcceptsNull(false)
  super.setImmutable(true)

  /** Reads bytes and returns a new object of the specified concrete type.
    * <p>
    * Before Kryo can be used to read child objects, {@link Kryo#reference(Object)} must be called with the parent object to
    * ensure it can be referenced by the child objects. Any serializer that uses {@link Kryo} to read a child object may need to
    * be reentrant.
    * <p>
    * This method should not be called directly, instead this serializer can be passed to {@link Kryo} read methods that accept a
    * serialier.
    *
    * @return May be null if { @link #getAcceptsNull()} is true. */
  
  override def read(kryo: Kryo, input: Input, `type`: Class[Model]): Model = {
    import ModelSerializerKryo._

    println("KRYO deserialization")
    val mType = input.readLong().asInstanceOf[Int]
    val bytes = Stream.continually(input.readByte()).takeWhile(_ != -1).toArray
    factories.get(mType) match {
      case Some(factory) => factory.restore(bytes)
      case _ => throw new Exception(s"Unknown model type $mType to restore")
    }
  }

  /** Writes the bytes for the object to the output.
    * <p>
    * This method should not be called directly, instead this serializer can be passed to {@link Kryo} write methods that accept a
    * serialier.
    *
    * @param value May be null if { @link #getAcceptsNull()} is true. */
  
  override def write(kryo: Kryo, output: Output, value: Model): Unit = {
    println("KRYO serialization")
    output.writeLong(value.getType)
    output.write(value.toBytes)
  }
}

object ModelSerializerKryo{
  private val factories = Map(ModelDescriptor.ModelType.PMML.value -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.value -> TensorFlowModel)
}

class ModelRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    kryo.register(classOf[Model], new ModelSerializerKryo())
  }
}
