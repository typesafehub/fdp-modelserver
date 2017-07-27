/**
  * Created by boris on 7/14/17.
  */
import sbt._
import Versions._

object Dependencies {
  val reactiveKafka = "com.typesafe.akka"               % "akka-stream-kafka_2.11"        % reactiveKafkaVersion
  
  val akkaStream    = "com.typesafe.akka"               % "akka-stream_2.11"              % akkaVersion
  val akkaHttp      = "com.typesafe.akka"               % "akka-http_2.11"                % akkaHttpVersion
  val akkaHttpJsonJackson = "de.heikoseeberger" % "akka-http-jackson_2.11" % akkaHttpJsonVersion  // exclude("com.fasterxml.jackson.module","jackson-module-scala_2.11")
  val akkaslf       = "com.typesafe.akka"               % "akka-slf4j_2.11"               % akkaSlfVersion


  val kafka         = "org.apache.kafka"                % "kafka_2.11"                    % kafkaVersion
  val kafkaclients  = "org.apache.kafka"                % "kafka-clients"                 % kafkaVersion
  val kafkastreams  = "org.apache.kafka"                % "kafka-streams"                 % kafkaVersion

  val curator       = "org.apache.curator"              % "curator-test"                  % Curator                 // ApacheV2

  val gson          = "com.google.code.gson"            % "gson"                          % gsonVersion
  val jersey        = "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % jerseyVersion
  val jerseymedia   = "org.glassfish.jersey.media"      % "jersey-media-json-jackson"     % jerseyVersion
  val jettyserver   = "org.eclipse.jetty"               % "jetty-server"                  % jettyVersion
  val jettyservlet  = "org.eclipse.jetty"               % "jetty-servlet"                 % jettyVersion
  val wsrs          = "javax.ws.rs"                     % "javax.ws.rs-api"               % wsrsVersion

  val tensorflow    = "org.tensorflow"                  % "tensorflow"                    % tensorflowVersion
  val tensorflowProto="org.tensorflow"                  % "proto"                         % tensorflowVersion

  val jpmml         = "org.jpmml"                       % "pmml-evaluator"                % PMMLVersion
  val jpmmlextras   = "org.jpmml"                       % "pmml-evaluator-extension"      % PMMLVersion

  val flinkScala    = "org.apache.flink"                % "flink-scala_2.11"              % flinkVersion
  val flinkStreaming= "org.apache.flink"                % "flink-streaming-scala_2.11"    % flinkVersion
  val flinkKafka    = "org.apache.flink"                % "flink-connector-kafka-0.10_2.11" % flinkVersion

  val joda          = "joda-time"                       % "joda-time"                     % jodaVersion

  val kryo          = "com.esotericsoftware.kryo"       % "kryo"                          % kryoVersion

  val sparkcore     = "org.apache.spark"                % "spark-core_2.11"               % sparkVersion
  val sparkstreaming= "org.apache.spark"                % "spark-streaming_2.11"          % sparkVersion
  val sparkkafka    = "org.apache.spark"                % "spark-streaming-kafka-0-10_2.11"% sparkVersion

  val scopt         = "com.github.scopt"                % "scopt_2.11"                    % scoptVersion
  val sparkML       = "org.apache.spark"                % "spark-mllib_2.11"              % sparkVersion
  val sparkJPMML    = "org.jpmml"                       % "jpmml-sparkml"                 % sparkPMMLVersion


  val modelsDependencies    = Seq(jpmml, jpmmlextras, tensorflow)
  val kafkabaseDependencies = Seq(reactiveKafka) ++ Seq(kafka, kafkaclients)
  val kafkaDependencies     = Seq(reactiveKafka) ++ Seq(kafka, kafkaclients, kafkastreams)
  val webDependencies       = Seq(gson, jersey, jerseymedia, jettyserver, jettyservlet, wsrs)
  val akkaServerDependencies = Seq(reactiveKafka) ++ Seq(akkaStream, akkaHttp, akkaHttpJsonJackson, reactiveKafka)

  val flinkDependencies = Seq(flinkScala, flinkStreaming, flinkKafka)

  val sparkDependencies = Seq(sparkcore, sparkstreaming, sparkkafka)

  val sparkMLDependencies = Seq(sparkML, sparkJPMML, scopt)


}
