package com.lightbend.spark.ml

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf
import org.jpmml.model.MetroJAXBUtil
import org.jpmml.sparkml.ConverterUtil


/**
  * Created by boris on 5/3/17.
  * Ordinary Linear regression
  *
  */

object WinequalityLinearRegression {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder
      .appName("WineQualityDecisionTreeClassifierPMML")
      .master("local")
      .getOrCreate()

    // Load and parse the data file.
    val df = spark.read
      .format("csv")
      .option("header", "true")
      .option("mode", "DROPMALFORMED")
      .option("delimiter", ";")
      .load("data/winequality_red_names.csv")
    val inputFields = List("fixed acidity", "volatile acidity", "citric acid", "residual sugar", "chlorides",
      "free sulfur dioxide", "total sulfur dioxide", "density", "pH", "sulphates", "alcohol")

    // CSV imports everything as Strings, fix the type
    val toDouble = udf[Double, String](_.toDouble)
    val dff = df.
      withColumn("quality", toDouble(df("quality"))).
      withColumn("fixed acidity", toDouble(df("fixed acidity"))). // 0
      withColumn("volatile acidity", toDouble(df("volatile acidity"))). // 1
      withColumn("citric acid", toDouble(df("citric acid"))). // 2
      withColumn("residual sugar", toDouble(df("residual sugar"))). // 3
      withColumn("chlorides", toDouble(df("chlorides"))). // 4
      withColumn("free sulfur dioxide", toDouble(df("free sulfur dioxide"))). // 5
      withColumn("total sulfur dioxide", toDouble(df("total sulfur dioxide"))). // 6
      withColumn("density", toDouble(df("density"))). // 7
      withColumn("pH", toDouble(df("pH"))). // 8
      withColumn("sulphates", toDouble(df("sulphates"))). // 9
      withColumn("alcohol", toDouble(df("alcohol"))) // 10

    // Regression operates on feature vectors not individual features, so convert to DF again
    val assembler = new VectorAssembler().
      setInputCols(inputFields.toArray).
      setOutputCol("features")

    val lr = new LinearRegression()
      .setMaxIter(50)
      .setRegParam(0.1)
      .setElasticNetParam(0.5)
      .setFeaturesCol("features")
      .setLabelCol("quality")

    // create pileline
    val pipeline = new Pipeline().setStages(Array(assembler,lr))

    // Fit the model
    //    val lrModel = lr.fit(dataFrame)
    val model = pipeline.fit(dff)
    val lrModel = model.stages(1).asInstanceOf[LinearRegressionModel]

    // Print the coefficients and intercept for linear regression
    println(s"Coefficients: ${lrModel.coefficients} Intercept: ${lrModel.intercept}")

    // Summarize the model over the training set and print out some metrics
    val trainingSummary = lrModel.summary
    println(s"numIterations: ${trainingSummary.totalIterations}")
    println(s"objectiveHistory: [${trainingSummary.objectiveHistory.mkString(",")}]")
    trainingSummary.residuals.show()
    println(s"RMSE: ${trainingSummary.rootMeanSquaredError}")
    println(s"r2: ${trainingSummary.r2}")

    // PMML
    val pmml = ConverterUtil.toPMML(dff.schema, model)
    MetroJAXBUtil.marshalPMML(pmml, System.out)
    spark.stop()
  }
}