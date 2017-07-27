package com.lightbend.modelserver.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer


/**
  * Created by blublins on 9/7/16.
  */
@SerialVersionUID(102L)
object KafkaSupport extends Serializable {

  // Kafka consumer properties
  private val sessionTimeout: Int = 10 * 1000
  private val connectionTimeout: Int = 8 * 1000
  private val AUTOCOMMITINTERVAL: String = "1000"
  // Frequency off offset commits
  private val SESSIONTIMEOUT: String = "30000"
  // The timeout used to detect failures - should be greater then processing time
  private val MAXPOLLRECORDS: String = "10"
  // Max number of records consumed in a single poll
  private val GROUPID: String = "Spark Streaming" // Consumer ID

  def getKafkaConsumerConfig(brokers: String): Map[String, String] = {
    Map[String, String](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> brokers,
      ConsumerConfig.GROUP_ID_CONFIG -> GROUPID,
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "true",
      ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG -> AUTOCOMMITINTERVAL,
      ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG -> SESSIONTIMEOUT,
      ConsumerConfig.MAX_POLL_RECORDS_CONFIG -> MAXPOLLRECORDS,
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[ByteArrayDeserializer].getTypeName,
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[ByteArrayDeserializer].getTypeName)
  }
}