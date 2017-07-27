package com.lightbend.tensorflow

/**
  * Created by boris on 5/26/17.
  */


import org.tensorflow.{SavedModelBundle, Session, Tensor}
import org.tensorflow.framework.{MetaGraphDef, SignatureDef, TensorInfo, TensorShapeProto}

import scala.collection.mutable._
import scala.collection.JavaConverters._
import scala.io.Source

object WineModelServingBundle {

  def apply(path: String, label: String): WineModelServingBundle = new WineModelServingBundle(path, label)
  def main(args: Array[String]): Unit = {
    val data_path = "data/winequality_red.csv"                  // data
    val saved_model_path = "data/WineQuality" // Saved model directory
    val label = "serve"
    val model = WineModelServingBundle(saved_model_path, label)
    val inputs = getListOfRecords(data_path)
    inputs.foreach(record =>
      println(s"result ${model.score(record._1)} expected ${record._2}"))
    model.cleanup()
  }

  def getListOfRecords(file: String): Seq[(Array[Float], Float)] = {

    var result = Seq.empty[(Array[Float], Float)]
    val bufferedSource = Source.fromFile(file)
    for (line <- bufferedSource.getLines) {
      val cols = line.split(";").map(_.trim)
      val record = cols.take(11).map(_.toFloat)
      result = (record, cols(11).toFloat) +: result
    }
    bufferedSource.close
    result
  }
}

class WineModelServingBundle(path : String, label : String){
  // Constructor

  println(s"Loading saved model from $path with label $label")
  val bundle = SavedModelBundle.load(path, label)
  val ls: Session = bundle.session
  val metaGraphDef = MetaGraphDef.parseFrom(bundle.metaGraphDef())
  val signatures = parseSignature(metaGraphDef.getSignatureDefMap.asScala)
  println("Model Loading complete")

  def score(record : Array[Float]) : Double = {
    val input = Tensor.create(Array(record))
    val result = ls.runner.feed(signatures(0).inputs(0).name, input).fetch(signatures(0).outputs(0).name).run().get(0)
    val rshape = result.shape
    var rMatrix = Array.ofDim[Float](rshape(0).asInstanceOf[Int],rshape(1).asInstanceOf[Int])
    result.copyTo(rMatrix)
    var value = (0, rMatrix(0)(0))
    1 to (rshape(1).asInstanceOf[Int] -1) foreach{i => {
      if(rMatrix(0)(i) > value._2)
        value = (i, rMatrix(0)(i))
    }}
    value._1.toDouble
  }

  def cleanup() : Unit = {
    ls.close
  }

  def convertParameters(tensorInfo: Map[String,TensorInfo]) : Seq[Parameter] = {

    var parameters = Seq.empty[Parameter]
    tensorInfo.foreach(input => {
      val fields = input._2.getAllFields.asScala
      var name = ""
      var dtype = ""
      var shape = Seq.empty[Int]
      fields.foreach(descriptor => {
        if(descriptor._1.getName.contains("shape") ){
          descriptor._2.asInstanceOf[TensorShapeProto].getDimList.toArray.map(d =>
            d.asInstanceOf[TensorShapeProto.Dim].getSize).toSeq.foreach(v => shape = shape :+ v.toInt)

        }
        if(descriptor._1.getName.contains("name") ) {
          name = descriptor._2.toString.split(":")(0)
        }
        if(descriptor._1.getName.contains("dtype") ) {
          dtype = descriptor._2.toString
        }
      })
      parameters = Parameter(name, dtype, shape) +: parameters
    })
    parameters
  }

  def parseSignature(signatureMap : Map[String, SignatureDef]) : Seq[Signature] = {

    var signatures = Seq.empty[Signature]
    signatureMap.foreach(definition => {
      val inputDefs = definition._2.getInputsMap.asScala
      val outputDefs = definition._2.getOutputsMap.asScala
      val inputs = convertParameters(inputDefs)
      val outputs = convertParameters(outputDefs)
      signatures = Signature(definition._1, inputs, outputs) +: signatures
    })
    signatures
  }
}

case class Parameter(name : String, dtype: String, dimensions: Seq[Int] = Seq.empty[Int]){}

case class Signature(name : String, inputs: Seq[Parameter], outputs: Seq[Parameter]){}