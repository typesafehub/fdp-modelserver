package com.lightbend.modelserver.typeschema

import java.io.IOException

import com.lightbend.model.modeldescriptor.ModelDescriptor
import org.apache.flink.api.common.typeutils.{CompatibilityResult, GenericTypeSerializerConfigSnapshot, TypeSerializer, TypeSerializerConfigSnapshot}
import com.lightbend.model.scala.Model
import com.lightbend.model.scala.PMML.PMMLModel
import com.lightbend.model.scala.tensorflow.TensorFlowModel
import org.apache.flink.core.memory.{DataInputView, DataOutputView}

class ModelTypeSerializer extends TypeSerializer[Option[Model]] {

  import ModelTypeSerializer._

  override def createInstance(): Option[Model] = None

  override def canEqual(obj: scala.Any): Boolean = obj.isInstanceOf[ModelTypeSerializer]

  override def duplicate(): TypeSerializer[Option[Model]] = new ModelTypeSerializer

  override def ensureCompatibility(configSnapshot: TypeSerializerConfigSnapshot): CompatibilityResult[Option[Model]] =
    CompatibilityResult.requiresMigration()

  override def serialize(record: Option[Model], target: DataOutputView): Unit = {
    record match {
      case Some(model) => {
        target.writeBoolean(true)
        val content = model.toBytes()
        target.writeLong(model.getType)
        target.writeLong(content.length)
        target.write(content)
      }
      case _ => target.writeBoolean(false)
    }
  }

  override def isImmutableType: Boolean = false

  override def getLength: Int = -1

  override def snapshotConfiguration(): TypeSerializerConfigSnapshot = new ModelSerializerConfigSnapshot

  override def copy(from: Option[Model]): Option[Model] =
    from match {
      case Some(model) => Some(factories.get(model.getType.asInstanceOf[Int]).get.restore(model.toBytes()))
      case _ => None
    }

  override def copy(from: Option[Model], reuse: Option[Model]): Option[Model] =
    from match {
      case Some(model) => Some(factories.get(model.getType.asInstanceOf[Int]).get.restore(model.toBytes()))
      case _ => None
    }

  override def copy(source: DataInputView, target: DataOutputView): Unit = {
    val exist = source.readBoolean()
    target.writeBoolean(exist)
    exist match {
      case true => {
        target.writeLong (source.readLong () )
        val clen = source.readLong ().asInstanceOf[Int]
        target.writeLong (clen)
        val content = new Array[Byte] (clen)
        source.read (content)
        target.write (content)
      }
      case _ =>
    }
  }

  override def deserialize(source: DataInputView): Option[Model] =
    source.readBoolean() match {
      case true => {
        val t = source.readLong().asInstanceOf[Int]
        val size = source.readLong().asInstanceOf[Int]
        val content = new Array[Byte] (size)
        source.read (content)
        Some(factories.get(t).get.restore(content))
      }
      case _ => None
    }

  override def deserialize(reuse: Option[Model], source: DataInputView): Option[Model] =
    source.readBoolean() match {
      case true => {
        val t = source.readLong().asInstanceOf[Int]
        val size = source.readLong().asInstanceOf[Int]
        val content = new Array[Byte] (size)
        source.read (content)
        Some(factories.get(t).get.restore(content))
      }
      case _ => None
    }

  override def equals(obj: scala.Any): Boolean = obj.isInstanceOf[ModelTypeSerializer]

  override def hashCode(): Int = 42
}

object ModelTypeSerializer{
  private val factories = Map(ModelDescriptor.ModelType.PMML.value -> PMMLModel,
    ModelDescriptor.ModelType.TENSORFLOW.value -> TensorFlowModel)

  def apply : ModelTypeSerializer = new ModelTypeSerializer()
}


object ModelSerializerConfigSnapshot {
  val VERSION = 1
}

class ModelSerializerConfigSnapshot[T <: Model]
                                extends TypeSerializerConfigSnapshot{

  import ModelSerializerConfigSnapshot._

//  def this() {this(classOf[T])}

  override def getVersion = VERSION

  var typeClass = classOf[Model]

  override def write(out: DataOutputView): Unit = {
    super.write(out)
    // write only the classname to avoid Java serialization
    out.writeUTF(classOf[Model].getName)
  }

  override def read(in: DataInputView): Unit = {
    super.read(in)
    val genericTypeClassname = in.readUTF
    try
      typeClass = Class.forName(genericTypeClassname, true, getUserCodeClassLoader).asInstanceOf[Class[Model]]
    catch {
      case e: ClassNotFoundException =>
        throw new IOException("Could not find the requested class " + genericTypeClassname + " in classpath.", e)
    }
  }

  def getTypeClass: Class[T] = typeClass.asInstanceOf[Class[T]]

  override def equals(obj: Any): Boolean = {
    if (obj == this) return true
    if (obj == null) return false
    (obj.getClass == getClass) && typeClass == obj.asInstanceOf[GenericTypeSerializerConfigSnapshot[_]].getTypeClass
  }

  override def hashCode: Int = 42
}
