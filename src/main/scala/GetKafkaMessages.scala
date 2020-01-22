//spark databox streaming project

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}


object GetKafkaMessages {

  val spark = SparkSession
    .builder()
    .master("yarn")
    .appName("KafkaSparkStreaming")
    .enableHiveSupport()
    .getOrCreate()

  spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsAccessKeyId","Your Access key id")
  spark.sparkContext.hadoopConfiguration.set("fs.s3n.awsSecretAccessKey","your secret access key")
  //spark.sparkContext.hadoopConfiguration.set("fs.s3n.impl",)

  def main(args: Array[String]): Unit = {


    val ssc = new StreamingContext(spark.sparkContext, Seconds(5))

    ssc.checkpoint("""/user/sparkdatabox/checkpoint""")

    val kafkaParams = Map(
      "bootstrap.servers" -> "wn01.itversity.com:6667,wn02.itversity.com:6667",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "streaming_demo_v2",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean))

    val preferredHosts = LocationStrategies.PreferConsistent

    val topics = Set("streaming_demo")

    val dstream = KafkaUtils.createDirectStream[String, String](ssc, preferredHosts, ConsumerStrategies.Subscribe[String, String](topics, kafkaParams))

    val lines = dstream.map(record => record.value())

    // Just Print the messages in the console

    /* lines.foreachRDD{ rdd =>
       if(rdd.count()>0){
         rdd.collect().foreach(println)
       }

     }*/

    // Read the Json message from Kafka , parse the Json and write it into hive table

/* lines.foreachRDD { rdd =>

   import spark.implicits._
   if (rdd.count() > 0) {
     val df: DataFrame = spark.sqlContext.read.json(rdd.toDS())
     // df.printSchema()
     //  df.show()

     val eventDataDF: DataFrame = df.select($"equipUnitInitCode", $"equipUnitNbr", $"tripId", $"customerId", $"fleetId", $"requestTime")
     eventDataDF.show()

     eventDataDF.write.format("hive").mode(SaveMode.Append).saveAsTable("power_training.event_data")

   }
 } */

 // Write into AWS s3 bucket

  lines.foreachRDD { rdd =>

   import spark.implicits._
   if (rdd.count() > 0) {
     val df: DataFrame = spark.sqlContext.read.json(rdd.toDS())
     // df.printSchema()
     //  df.show()

     val eventDataDF: DataFrame = df.select($"equipUnitInitCode", $"equipUnitNbr", $"tripId", $"customerId", $"fleetId", $"requestTime")
     eventDataDF.show()

     eventDataDF.coalesce(1).write.format("csv").option("header","true").mode(SaveMode.Append).save("s3n://sd-training-v1/spark/events")

   }
 }

 ssc.start()
 ssc.awaitTermination()
}

}
