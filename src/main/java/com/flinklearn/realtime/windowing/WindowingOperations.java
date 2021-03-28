package com.flinklearn.realtime.windowing;

import com.flinklearn.realtime.datastreamapi.AuditTrail;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.KafkaStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.AuditTrail;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import org.apache.flink.streaming.api.datastream.DataStream;

import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.api.common.typeinfo.Types;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class WindowingOperations {

    public static void main(String[] args) {

        try {

            /*******************************************************************
             * Set up Flink environment
             *******************************************************************/
            // Create stream execution environment
            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();

            // Configure Kafka properties
            Properties properties = new Properties();
            properties.setProperty("bootstrap.servers", "localhost:9092");
            properties.setProperty("group.id", "flink.learn.realtime");

            // Set up Flink Kafka consumer
            FlinkKafkaConsumer<String> kafkaConsumer =
                    new FlinkKafkaConsumer<String>(
                            "flink.kafka.streaming.source",
                            new SimpleStringSchema(),
                            properties
                    );
            kafkaConsumer.setStartFromLatest();

            // Add Kafka consumer as Flink stream source
            DataStream<String> auditStr = streamEnv.addSource(kafkaConsumer);

            // Convert each string record to an AuditTrail object and print
            DataStream<AuditTrail> auditObj = auditStr.map(
                    new MapFunction<String, AuditTrail>() {
                        @Override
                        public AuditTrail map(String atStr) throws Exception {
                            System.out.println("--- Received record: "+ atStr);
                            AuditTrail atObj = new AuditTrail(atStr);
                            return atObj;
                        }
            });

            /*******************************************************************
             * Use sliding windows
             *******************************************************************/
            // Sliding window interval of 10 seconds, sliding by 5 seconds
            DataStream<Tuple4<String, Integer, Long, Long>> slidingSummary =
                    auditObj.map(i -> new Tuple4<String, Integer, Long, Long>(
                            String.valueOf(System.currentTimeMillis()),
                            1,
                            i.getTimestamp(),
                            i.getTimestamp()
                    ))
                    .returns(Types.TUPLE(
                            Types.STRING,
                            Types.INT,
                            Types.LONG,
                            Types.LONG
                    ))
                    .timeWindowAll(
                            Time.seconds(10),
                            Time.seconds(5)
                    )
                    .reduce((x, y) -> new Tuple4<String, Integer, Long, Long>(
                            x.f0,
                            x.f1 + y.f1,
                            Math.min(x.f2, y.f2),
                            Math.max(x.f3, y.f3)
                    ));

            // Pretty print the tuples
            slidingSummary.map(new MapFunction<Tuple4<String, Integer, Long, Long>, Object>() {

                @Override
                public Object map(Tuple4<String, Integer, Long, Long> slidingSummary) throws Exception {
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                    String minTime = format.format(new Date(Long.valueOf(slidingSummary.f2)));
                    String maxTime = format.format(new Date(Long.valueOf(slidingSummary.f3)));
                    System.out.println(
                            "Sliding Summary : "
                            + (new Date()).toString()
                            + " Start Time : " + minTime
                            + " End Time : " + maxTime
                            + " Count : " + slidingSummary.f1
                    );
                    return null;
                }
            });


            /*******************************************************************
             * Set up data source and execute
             *******************************************************************/
            //Start the Kafka Stream generator on a separate thread
            Utils.printHeader("Starting Kafka Data Generator...");
            Thread kafkaThread = new Thread(new KafkaStreamDataGenerator());
            kafkaThread.start();

            // execute the streaming pipeline
            streamEnv.execute("Flink Kafka consumer and windowing");

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
