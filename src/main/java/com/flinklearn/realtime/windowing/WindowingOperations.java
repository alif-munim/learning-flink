package com.flinklearn.realtime.windowing;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.KafkaStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.AuditTrail;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import org.apache.flink.streaming.api.datastream.DataStream;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class WindowingOperations {

    public static void main(String[] args) {

        try {

            /*******************************************************************
             * Set up Flink environment
             *******************************************************************/

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
