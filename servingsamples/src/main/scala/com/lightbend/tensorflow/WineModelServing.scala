package com.lightbend.tensorflow

import java.nio.file.{Files, Path, Paths}
import scala.io.Source
import org.tensorflow.{Graph, Session, Tensor}

/**
  * Created by boris on 5/25/17.
  */
class WineModelServing(path : String) {
  import WineModelServing._

  // Constructor
  println(s"Loading saved model from $path")
  val lg = readGraph(Paths.get (path))
  val ls = new Session (lg)
  println("Model Loading complete")

  def score(record : Array[Float]) : Double = {
    val input = Tensor.create(Array(record))
    val result = ls.runner.feed("dense_1_input", input).fetch("dense_3/Sigmoid").run().get(0)
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
}

object WineModelServing{
  def main(args: Array[String]): Unit = {
    val model_path = "data/optimized_WineQuality.pb"                      // model
    val data_path = "data/winequality_red.csv"                            // data

    val lmodel = new WineModelServing(model_path)
    val inputs = getListOfRecords(data_path)
    inputs.foreach(record =>
      println(s"result ${lmodel.score(record._1)} expected ${record._2}"))
    lmodel.cleanup()
  }

  private def readGraph(path: Path) : Graph = {
    try {
      val graphData = Files.readAllBytes(path)
      val g = new Graph
      g.importGraphDef(graphData)
      g
    } catch {
      case e: Throwable =>
        println("Failed to read graph [" + path + "]: " + e.getMessage)
        System.exit(1)
        null.asInstanceOf[Graph]
    }
  }

  def getListOfRecords(file: String): Seq[(Array[Float], Float)] = {

    var result = Seq.empty[(Array[Float], Float)]
    val bufferedSource = Source.fromFile(file)
    try for (line <- bufferedSource.getLines) {
      val cols = line.split(";").map(_.trim)
      val record = cols.take(11).map(_.toFloat)
      result = (record, cols(11).toFloat) +: result
    } finally
      bufferedSource.close
    result
  }

}