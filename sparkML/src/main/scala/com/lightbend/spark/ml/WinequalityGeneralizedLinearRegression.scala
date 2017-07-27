package com.lightbend.spark.ml

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{GeneralizedLinearRegression, GeneralizedLinearRegressionModel}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf
import org.jpmml.model.MetroJAXBUtil
import org.jpmml.sparkml.ConverterUtil


/**
  * Created by boris on 5/3/17.
  *
  * Contrasted with linear regression where the output is assumed to follow a Gaussian distribution, generalized
  * linear models (GLMs) are specifications of linear models where the response variable YiYi follows some
  * distribution from the exponential family of distributions. Spark’s GeneralizedLinearRegression interface allows
  * for flexible specification of GLMs which can be used for various types of prediction problems including linear
  * regression, Poisson regression, logistic regression, and others. Currently in spark.ml, only a following subset
  * of the exponential family distributions are supported:
  *     Family	  Response Type	  Supported Links
  *     Gaussian	Continuous	    Identity*, Log, Inverse
  *     Binomial	Binary	        Logit*, Probit, CLogLog
  *     Poisson	  Count	          Log*, Identity, Sqrt
  *     Gamma	    Continuous	    Inverse*, Idenity, Log
  */

object WinequalityGeneralizedLinearRegression {

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

    val lr = new GeneralizedLinearRegression()
//    .setFamily("gaussian")
      .setFamily("gamma")
      .setLink("identity")
      .setMaxIter(100)
      .setRegParam(0.3)
      .setFeaturesCol("features")
      .setLabelCol("quality")

    // create pileline
    val pipeline = new Pipeline().setStages(Array(assembler,lr))

    // Fit the model
    //    val lrModel = lr.fit(dataFrame)
    val model = pipeline.fit(dff)
    val lrModel = model.stages(1).asInstanceOf[GeneralizedLinearRegressionModel]


    // Summarize the model over the training set and print out some metrics
    // Print the coefficients and intercept for generalized linear regression model
    println(s"Coefficients: ${lrModel.coefficients}")
    println(s"Intercept: ${lrModel.intercept}")

    // Summarize the model over the training set and print out some metrics
    val summary = lrModel.summary
    println(s"Coefficient Standard Errors: ${summary.coefficientStandardErrors.mkString(",")}")
    println(s"T Values: ${summary.tValues.mkString(",")}")
    println(s"P Values: ${summary.pValues.mkString(",")}")
    println(s"Dispersion: ${summary.dispersion}")
    println(s"Null Deviance: ${summary.nullDeviance}")
    println(s"Residual Degree Of Freedom Null: ${summary.residualDegreeOfFreedomNull}")
    println(s"Deviance: ${summary.deviance}")
    println(s"Residual Degree Of Freedom: ${summary.residualDegreeOfFreedom}")
    println(s"AIC: ${summary.aic}")
    println("Deviance Residuals: ")
    summary.residuals().show()

    // PMML
    val pmml = ConverterUtil.toPMML(dff.schema, model)
    MetroJAXBUtil.marshalPMML(pmml, System.out)
    spark.stop()
  }
}