package com.flinklearn.realtime.project;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.BrowserStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.BrowserEvent;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

/****************************************************************************
 * Create flink pipeline which does the following:
 * (1) Compute 10-second summaries containing user, action, and event count
 * (2) Compute the duration for each action and print user, action, duration
 ****************************************************************************/

public class SummaryDuration {

    public static void main(String[] args) {

        try {

            /****************************************************************************
             * Create stream execution environment
             ****************************************************************************/
            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();
            streamEnv.setParallelism(1);

            /****************************************************************************
             * Configure and create flink kafka consumer
             ****************************************************************************/
            Properties properties = new Properties();
            properties.put("bootstrap.servers", "localhost:9092");
            properties.put("group.id", "flink.learn.realtime");

            FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<String>(
                    "flink.summary.duration",
                    new SimpleStringSchema(),
                    properties
            );
            kafkaConsumer.setStartFromLatest();

            /****************************************************************************
             * Add consumer as a streaming source and convert to browser obj data stream
             ****************************************************************************/
            DataStream<String> browserEventString = streamEnv.addSource(kafkaConsumer);

            DataStream<BrowserEvent> browserEventObject = browserEventString.map(
                    new MapFunction<String, BrowserEvent>() {
                        @Override
                        public BrowserEvent map(String bStr) throws Exception {
                            System.out.println("* Received new event: " + bStr);
                            return new BrowserEvent(bStr);
                        }
                    }
            );

            /****************************************************************************
             * Output 10-second summaries using event time operations
             ****************************************************************************/

            /****************************************************************************
             * Perform stateful operations to print event durations
             ****************************************************************************/

            /****************************************************************************
             * Start browser event thread and execute pipeline
             ****************************************************************************/
            Utils.printHeader("Starting browser event streaming...");
            Thread browserThread = new Thread(new BrowserStreamDataGenerator());
            browserThread.start();

            streamEnv.execute("Starting flink summary and duration pipeline...");

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
