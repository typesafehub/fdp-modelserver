package com.lightbend.modelserver

import scala.util.Try
import com.lightbend.model.modeldescriptor.ModelDescriptor
import com.lightbend.model.winerecord.WineRecord

/**
  * Created by boris on 5/8/17.
  */

object DataRecord {

  def fromByteArray(message: Array[Byte]): Try[WineRecord] = Try {
    WineRecord.parseFrom(message)
  }
}
