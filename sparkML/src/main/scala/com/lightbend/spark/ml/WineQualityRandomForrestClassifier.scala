package com.lightbend.spark.ml

/**
  * Created by boris on 5/1/17.
  *
  * Random forests or random decision forests are an ensemble learning method for classification, regression and
  * other tasks, that operate by constructing a multitude of decision trees at training time and outputting the
  * class that is the mode of the classes (classification) or mean prediction (regression) of the individual trees.
  * Random decision forests correct for decision trees' habit of overfitting to their training set.
  */

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorAssembler}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.jpmml.model.MetroJAXBUtil
import org.jpmml.sparkml.ConverterUtil


object WineQualityRandomForrestClassifierPMML {

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
    val toDouble = udf[Double, String]( _.toDouble)
    val dff = df.
      withColumn("fixed acidity",        toDouble(df("fixed acidity"))).          // 0  +
      withColumn("volatile acidity",     toDouble(df("volatile acidity"))).       // 1  +
      withColumn("citric acid",          toDouble(df("citric acid"))).            // 2  -
      withColumn("residual sugar",       toDouble(df("residual sugar"))).         // 3  +
      withColumn("chlorides",            toDouble(df("chlorides"))).              // 4  -
      withColumn("free sulfur dioxide",  toDouble(df("free sulfur dioxide"))).    // 5  +
      withColumn("total sulfur dioxide", toDouble(df("total sulfur dioxide"))).   // 6  +
      withColumn("density",              toDouble(df("density"))).                // 7  -
      withColumn("pH",                   toDouble(df("pH"))).                     // 8  +
      withColumn("sulphates",            toDouble(df("sulphates"))).              // 9  +
      withColumn("alcohol",              toDouble(df("alcohol")))                 // 10 +


    // Decision Tree operates on feature vectors not individual features, so convert to DF again
    val assembler = new VectorAssembler().
      setInputCols(inputFields.toArray).
      setOutputCol("features")

    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol("quality")
      .setOutputCol("indexedLabel")
      .fit(dff)

    // Train a DecisionTree model.
    val dt = new RandomForestClassifier()
      .setLabelCol("indexedLabel")
      .setFeaturesCol("features")
      .setNumTrees(10)

    // Convert indexed labels back to original labels.
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // create pileline
    val pipeline = new Pipeline()
      .setStages(Array(assembler, labelIndexer, dt, labelConverter))

    // Train model
    val model = pipeline.fit(dff)

    // Print results
    val treeModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    println("Learned classification tree model:\n" + treeModel.toDebugString)

    // PMML
    val schema = dff.schema
    val pmml = ConverterUtil.toPMML(schema, model)
    MetroJAXBUtil.marshalPMML(pmml, System.out)
    spark.stop()
  }
}