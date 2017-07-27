package com.lightbend.model.scala

import com.lightbend.model.winerecord.WineRecord
import com.lightbend.modelserver.support.scala.ModelToServe

/**
  * Created by boris on 5/8/17.
  */

case class DataWithModel(model: Option[ModelToServe], data : Option[WineRecord]){
  def isModel : Boolean = model.isDefined
  def getModel : ModelToServe = model.get
  def getData : WineRecord = data.get
  def getDataType : String = {
    if(isModel)
      getModel.dataType
    else
      getData.dataType
  }
}