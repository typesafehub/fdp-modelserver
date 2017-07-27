package com.lightbend.modelserver.query

import java.util.concurrent.{Executors, TimeUnit}

import com.lightbend.modelserver.support.scala.ModelToServeStats
import org.apache.flink.api.common.{ExecutionConfig, JobID}
import org.apache.flink.api.scala.createTypeInformation
import org.apache.flink.configuration.{Configuration, JobManagerOptions}
import org.apache.flink.runtime.highavailability.HighAvailabilityServicesUtils
import org.apache.flink.runtime.query.QueryableStateClient
import org.apache.flink.runtime.query.netty.message.KvStateRequestSerializer
import org.apache.flink.runtime.state.{VoidNamespace, VoidNamespaceSerializer}
import org.joda.time.DateTime

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

/**
  * Created by boris on 5/12/17.
  * see https://ci.apache.org/projects/flink/flink-docs-release-1.3/dev/stream/queryable_state.html
  * It uses default port 6123 to access Flink server
  */
object ModelStateQuery {

  val timeInterval = 1000 * 20        // 20 sec

  def main(args: Array[String]) {

    val jobId = JobID.fromHexString("817bfacb1f0317eb15fb20c1201b9e1a")
    val types = Array("wine")

    val config = new Configuration()
    config.setString(JobManagerOptions.ADDRESS, "localhost")
    config.setInteger(JobManagerOptions.PORT, 6124)

    val highAvailabilityServices = HighAvailabilityServicesUtils.createHighAvailabilityServices(
      config, Executors.newSingleThreadScheduledExecutor, HighAvailabilityServicesUtils.AddressResolution.TRY_ADDRESS_RESOLUTION)
    val client = new QueryableStateClient(config, highAvailabilityServices)

    val execConfig = new ExecutionConfig
    val keySerializer = createTypeInformation[String].createSerializer(execConfig)
    val valueSerializer = createTypeInformation[ModelToServeStats].createSerializer(execConfig)

    println("                   Name                      |       Description       |       Since       |       Average       |       Min       |       Max       |")
    while(true) {
      val stats = for (key <- types) yield {
        val serializedKey = KvStateRequestSerializer.serializeKeyAndNamespace(
          key, keySerializer,
          VoidNamespace.INSTANCE, VoidNamespaceSerializer.INSTANCE)

        // now wait for the result and return it
        try {
          val serializedResult = client.getKvState(jobId, "currentModel", key.hashCode(), serializedKey)
          val serializedValue = Await.result(serializedResult, FiniteDuration(2, TimeUnit.SECONDS))
          val value = KvStateRequestSerializer.deserializeValue(serializedValue, valueSerializer)
          List(value.name, value.description, value.since, value.usage, value.duration, value.min, value.max)
        } catch {
          case e: Exception => {
            e.printStackTrace()
            List()
          }
        }
      }
      stats.toList.filter(_.nonEmpty).foreach(row =>
        println(s" ${row(0)} | ${row(1)} | ${new DateTime(row(2)).toString("yyyy/MM/dd HH:MM:SS")} | ${row(3)} |" +
          s" ${row(4).asInstanceOf[Double]/row(3).asInstanceOf[Long]} | ${row(5)} | ${row(6)} |")
      )
      Thread.sleep(timeInterval)
    }
  }
}