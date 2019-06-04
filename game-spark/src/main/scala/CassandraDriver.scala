class CassandraDriver extends SparkSessionBuilder {

  val spark = buildSparkSession
  import spark.implicits._
  val connector = CassandraConnector(spark.sparkContext.getConf)

  val namespace = "slotty"
  val foreachTableSink = "spark_struct_stream_sink"
}