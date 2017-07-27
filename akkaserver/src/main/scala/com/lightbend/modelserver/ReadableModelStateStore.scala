package com.lightbend.modelserver.modelServer

import com.lightbend.modelserver.support.scala.ModelToServeStats


/**
  * Created by boris on 7/21/17.
  */
trait ReadableModelStateStore {
  def getCurrentServingInfo: ModelToServeStats
}

