package com.lightbend.jpmml

import java.io.{FileInputStream, InputStream}

import org.dmg.pmml.{FieldName, PMML}
import org.jpmml.evaluator.visitors.{ExpressionOptimizer, _}
import org.jpmml.evaluator.{Computable, FieldValue, ModelEvaluatorFactory, TargetField}
import org.jpmml.model.PMMLUtil

import scala.collection.JavaConversions._
import scala.collection._
import scala.io.Source

/**
  * Created by boris on 5/2/17.
  * Test of JPMML evaluator for the model created by DecisionTreeClassificator in SparkML and exported
  * using JPMML Spark https://github.com/jpmml/jpmml-spark
  * Implementation is based on JPMML example at
  * https://github.com/jpmml/jpmml-evaluator/blob/master/pmml-evaluator-example/src/main/java/org/jpmml/evaluator/EvaluationExample.java
  */


class WineQualityRandomForestClassifier(path : String) {
  import WineQualityRandomForestClassifier._

  var arguments = mutable.Map[FieldName, FieldValue]()

  // constructor
  val pmml: PMML = readPMML(path)
  optimize(pmml)

  // Create and verify evaluator
  val evaluator = ModelEvaluatorFactory.newInstance().newModelEvaluator(pmml)
  evaluator.verify()

  // Get input/target fields
  val inputFields = evaluator.getInputFields
  val target: TargetField = evaluator.getTargetFields.get(0)
  val tname = target.getName

  def score(record : Array[Float]) : Double = {
    arguments.clear()
    inputFields.foreach(field => {
      arguments.put(field.getName, field.prepare(getValueByName(record, field.getName.getValue)))
    })

    // Calculate Output// Calculate Output
    val result = evaluator.evaluate(arguments)

    // Prepare output
    result.get(tname) match {
      case c : Computable => c.getResult.toString.toDouble
      case v : Any => v.asInstanceOf[Double]
    }
  }

  private def getValueByName(inputs : Array[Float], name: String) : Double = {
    names.get(name) match {
      case Some(index) => {
        val v = inputs(index)
        v.asInstanceOf[Double]
      }
      case _ =>.0
    }
  }
}

object WineQualityRandomForestClassifier {

  def main(args: Array[String]): Unit = {
    val model_path = "data/winequalityRandonForrestClassification.pmml"   // model
    val data_path = "data/winequality_red.csv" // data
    val lmodel = new WineQualityRandomForestClassifier(model_path)

    val inputs = getListOfRecords(data_path)
    inputs.foreach(record =>
      println(s"result ${lmodel.score(record._1)} expected ${record._2}"))
  }

  def readPMML(file: String): PMML = {
    var is = null.asInstanceOf[InputStream]
    try {
      is = new FileInputStream(file)
      PMMLUtil.unmarshal(is)
    }
    finally if (is != null) is.close()
  }

  private val optimizers = Array(new ExpressionOptimizer, new FieldOptimizer, new PredicateOptimizer,
    new GeneralRegressionModelOptimizer, new NaiveBayesModelOptimizer, new RegressionModelOptimizer)

  def optimize(pmml : PMML) = this.synchronized {
    optimizers.foreach(opt =>
      try {
        opt.applyTo(pmml)
      } catch {
        case t: Throwable => {
          println(s"Error optimizing model for optimizer $opt")
          t.printStackTrace()
        }
      }
    )
  }

  def getListOfRecords(file: String): Seq[(Array[Float], Float)] = {

    var result = Seq.empty[(Array[Float], Float)]
    val bufferedSource = Source.fromFile(file)
    var current = 0
    for (line <- bufferedSource.getLines) {
      if (current == 0)
        current = 1
      else {
        val cols = line.split(";").map(_.trim)
        val record = Array(
          cols(0).toFloat, cols(1).toFloat, cols(2).toFloat, cols(3).toFloat, cols(4).toFloat,
          cols(5).toFloat, cols(6).toFloat, cols(7).toFloat, cols(8).toFloat, cols(9).toFloat, cols(10).toFloat)
        result = (record, cols(11).toFloat) +: result
      }
    }
    bufferedSource.close
    result
  }

  private val names = Map("fixed acidity" -> 0,
    "volatile acidity" -> 1,"citric acid" ->2,"residual sugar" -> 3,
    "chlorides" -> 4,"free sulfur dioxide" -> 5,"total sulfur dioxide" -> 6,
    "density" -> 7,"pH" -> 8,"sulphates" ->9,"alcohol" -> 10)
}
