package com.lightbend.modelserver.typeschema

/**
  * Created by boris on 5/9/17.
  */
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.streaming.util.serialization.{DeserializationSchema, SerializationSchema}

class ByteArraySchema extends DeserializationSchema[Array[Byte]] with SerializationSchema[Array[Byte]] {

  private val serialVersionUID: Long = 1234567L

  override def isEndOfStream(nextElement: Array[Byte]): Boolean = false

  override def deserialize(message: Array[Byte]): Array[Byte] = message

  override def serialize(element: Array[Byte]): Array[Byte] = element

  override def getProducedType: TypeInformation[Array[Byte]] =
    //PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO
    TypeExtractor.getForClass(classOf[Array[Byte]])
}
