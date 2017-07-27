package com.lightbend.modelserver.queriablestate


import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import com.lightbend.modelserver.modelServer.ReadableModelStateStore
import com.lightbend.modelserver.support.scala.ModelToServeStats
import de.heikoseeberger.akkahttpjackson.JacksonSupport

object QueriesAkkaHttpResource extends JacksonSupport {

  def storeRoutes(predictions: ReadableModelStateStore): Route =
    get {
      path("stats") {
        val info: ModelToServeStats = predictions.getCurrentServingInfo
        complete(info)
      }
    }
}
