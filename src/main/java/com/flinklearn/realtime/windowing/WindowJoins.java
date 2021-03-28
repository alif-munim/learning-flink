package com.flinklearn.realtime.windowing;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import com.flinklearn.realtime.datasource.KafkaStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.AuditTrail;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.Properties;
import java.util.stream.Stream;

public class WindowJoins {

    public static void main(String[] args) {

        try {

            /*******************************************************************
             * Set up stream execution environment
             *******************************************************************/

            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();

            /*******************************************************************
             * Read CSV file into datastream
             *******************************************************************/

            String dataDir = "data/raw_audit_trail";
            TextInputFormat fileFormat = new TextInputFormat(
                    new Path(dataDir)
            );
            DataStream<String> fileString = streamEnv.readFile(
                    fileFormat,
                    dataDir,
                    FileProcessingMode.PROCESS_CONTINUOUSLY,
                    1000
            );

            // Map string datastream to audit trail obj
            DataStream<AuditTrail> fileObj = fileString.map(
                    new MapFunction<String, AuditTrail>() {
                        @Override
                        public AuditTrail map(String aStr) throws Exception {
                            System.out.println("--- Received file record: " + aStr);
                            AuditTrail aObj = new AuditTrail(aStr);
                            return aObj;
                        }
                    }
            );

            /*******************************************************************
             * Read Kafka records into datastream
             *******************************************************************/

            Properties properties = new Properties();
            properties.setProperty("bootstrap.servers", "localhost:9092");
            properties.setProperty("group.id", "flink.learn.realtime");

            FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<String>(
                    "flink.kafka.streaming.source",
                    new SimpleStringSchema(),
                    properties
            );
            kafkaConsumer.setStartFromLatest();

            DataStream<String> kafkaString = streamEnv.addSource(kafkaConsumer);

            DataStream<AuditTrail> kafkaObj = kafkaString.map(
                    new MapFunction<String, AuditTrail>() {
                        @Override
                        public AuditTrail map(String kStr) throws Exception {
                            System.out.println("--- Received kafka record: " + kStr);
                            AuditTrail aObj = new AuditTrail(kStr);
                            return aObj;
                        }
                    }
            );

            /*******************************************************************
             * Join both streams based on window
             *******************************************************************/
            DataStream<Tuple2<String, Integer>> joinCounts =
                    fileObj.join(kafkaObj)
                    .where(new KeySelector<AuditTrail, String>() {
                        @Override
                        public String getKey(AuditTrail fileTrail) throws Exception {
                            return fileTrail.getUser();
                        }
                    })
                    .equalTo(new KeySelector<AuditTrail, String>() {
                        @Override
                        public String getKey(AuditTrail kafkaTrail) throws Exception {
                            return kafkaTrail.getUser();
                        }
                    })
                    .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
                    .apply(new JoinFunction<AuditTrail, AuditTrail, Tuple2<String, Integer>>() {
                        @Override
                        public Tuple2<String, Integer> join(AuditTrail fileTrail, AuditTrail kafkaTrail) throws Exception {
                            return new Tuple2<String, Integer>(
                                fileTrail.getUser(),
                                1
                            );
                        }
                    });

                    // Print the counts
                    joinCounts.print();

            /*******************************************************************
             * Kick off data sources and execute stream environment
             *******************************************************************/
            // Start file stream data generator
            Utils.printHeader("Starting file stream data generator...");
            Thread fileThread = new Thread(new FileStreamDataGenerator());
            fileThread.start();

            // Start kafka stream record generator
            Utils.printHeader("Starting kafka stream record generator");
            Thread kafkaThread = new Thread(new KafkaStreamDataGenerator());
            kafkaThread.start();

            // Execute stream environment
            streamEnv.execute("Starting Flink streaming job...");

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
