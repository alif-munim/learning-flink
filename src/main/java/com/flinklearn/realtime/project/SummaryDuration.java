package com.flinklearn.realtime.project;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.BrowserStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.BrowserEvent;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import javax.annotation.Nullable;
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
            // Create event time watermarked stream
            DataStream<BrowserEvent> browserEventWatermarked = browserEventObject
                    .assignTimestampsAndWatermarks(
                            new AssignerWithPunctuatedWatermarks<BrowserEvent>() {

                                transient long currentWatermark = 0L;
                                int delay = 10000;
                                int buffer = 2000;

                                @Nullable
                                @Override
                                public Watermark checkAndGetNextWatermark(BrowserEvent browserEvent, long nextMark) {

                                    long currentTime = System.currentTimeMillis();

                                    if(currentWatermark == 0L) {
                                        currentWatermark = currentTime;
                                    } else if(currentTime - currentWatermark > delay) {
                                        currentWatermark = currentTime;
                                    }

                                    return new Watermark(currentWatermark - buffer);

                                }

                                @Override
                                public long extractTimestamp(BrowserEvent browserEvent, long prevMark) {
                                    return browserEvent.getTimestamp();
                                }
                            }
                    );

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
