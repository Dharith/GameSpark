/*
 *
 * Instaclustr (www.instaclustr.com)
 * Kafka, Spark Streaming and Cassandra example
 *
 */

import java.io.FileReader
import java.util.Properties

import scala.collection.JavaConversions._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import com.datastax.spark.connector._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import com.datastax.driver.core.Cluster
import java.util.Date

object KafkaSparkCassandra {

  def main(args: Array[String]) {

    // read the configuration file
    val sparkConf = new SparkConf().setAppName("Game")

    val cassandra_host = sparkConf.set("spark.cassandra.connection.host", "54.169.77.149")
                            .set("spark.executor.memory","1g")
                            .set("")

    val cluster = Cluster.builder().addContactPoint(cassandra_host).withCredentials(cassandra_user, cassandra_pass).build()
    val session = cluster.connect()
    session.close()

    val sparkStreamContext = new StreamingContext(sparkConf, Seconds(5))
    sparkStreamContext.sparkContext.setLogLevel("ERROR")

    val timer = new Thread() {
      override def run() {
        Thread.sleep(1000 * 30)
        sparkStreamContext.stop()
      }
    }

    val kafkaProps = new Properties()
    kafkaProps.load(new FileReader("kafka.properties"))
    val kafkaParams = kafkaProps.toMap[String, String]

    kafkaProps.put("bootstrap.servers", "kafka1.joker.local:9092");
    kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaProps.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");

    val producer = new KafkaProducer[String,String](kafkaProps);
    val record = new ProducerRecord[String, String](topic, "key", "value")
    producer.send(record)
    producer.close()

    val topicsSet = Set[String]("gameTransaction")
    val messages = KafkaUtils.createDirectStream(sparkStreamContext,PreferConsistent,Subscribe[String,String](topicsSet,kafkaParams))

    sparkStreamContext.start() // start the streaming context
    timer.start()
    sparkStreamContext.awaitTermination() // block while the context is running
    sparkStreamContext.stop()

    val sc = new SparkContext(sparkConf)
    val rdd1 = sc.cassandraTable("slotty", "game_transaction_by_upline")
    rdd1.take(100).foreach(println)
    sc.stop()

    System.exit(0)
  }
}
