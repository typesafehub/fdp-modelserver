
name := "ModelServing"

version := "1.0"

scalaVersion in ThisBuild := "2.11.11"


lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      PB.gens.java -> (sourceManaged in Compile).value,
      scalapb.gen(javaConversions=true) -> (sourceManaged in Compile).value
    )
  )

lazy val kafkaclient = (project in file("./kafkaclient"))
  .settings(libraryDependencies ++= Dependencies.kafkabaseDependencies)
  .dependsOn(protobufs, kafkaconfiguration)

lazy val model = (project in file("./model"))
  .settings(libraryDependencies ++= Dependencies.modelsDependencies)
  .dependsOn(protobufs, utils)


lazy val kafkastreamsserver = (project in file("./Kafkastreamsserver"))
  .settings(libraryDependencies ++= Dependencies.kafkaDependencies ++ Dependencies.webDependencies)
  .dependsOn(model, kafkaconfiguration, utils)

lazy val akkaServer = (project in file("./akkaserver"))
  .settings(libraryDependencies ++= Dependencies.kafkaDependencies ++ Dependencies.akkaServerDependencies
    ++ Dependencies.modelsDependencies)
  .dependsOn(model, kafkaconfiguration, utils)

lazy val flinkserver = (project in file("./flinkserver"))
  .settings(libraryDependencies ++= Dependencies.flinkDependencies ++ Seq(Dependencies.joda, Dependencies.akkaslf))
  .settings(dependencyOverrides += "com.typesafe.akka" % "akka-actor-2.11" % "2.3")
  .dependsOn(model, kafkaconfiguration, utils)

lazy val sparkserver = (project in file("./sparkserver"))
  .settings(libraryDependencies ++= Dependencies.sparkDependencies)
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.9")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.9")
  .settings(dependencyOverrides += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.9")
  .dependsOn(model, kafkaconfiguration, utils)

lazy val sparkML = (project in file("./sparkML"))
  .settings(libraryDependencies ++= Dependencies.sparkMLDependencies)
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.9")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.9")
  .settings(dependencyOverrides += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.8.9")


lazy val servingsamples = (project in file("./servingsamples"))
  .settings(libraryDependencies ++= Dependencies.modelsDependencies ++ Seq(Dependencies.tensorflowProto))


lazy val kafkaconfiguration = (project in file("./kafkaconfiguration"))

lazy val utils = (project in file("./utils"))
  .settings(libraryDependencies ++= Dependencies.kafkaDependencies ++ Seq(Dependencies.curator))
  .dependsOn(protobufs)

lazy val root = (project in file(".")).
  aggregate(protobufs, kafkaclient, model, utils, kafkaconfiguration, kafkastreamsserver, akkaServer, sparkserver)

