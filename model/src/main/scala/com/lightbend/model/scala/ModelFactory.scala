package com.lightbend.model.scala

import com.lightbend.modelserver.support.scala.ModelToServe


/**
  * Created by boris on 5/9/17.
  * Basic trait for model factory
  */
trait ModelFactory {
  def create(input : ModelToServe) : Option[Model]
  def restore(bytes : Array[Byte]) : Model
}