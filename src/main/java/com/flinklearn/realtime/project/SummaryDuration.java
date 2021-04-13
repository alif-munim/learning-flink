package com.flinklearn.realtime.project;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.BrowserStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.BrowserEvent;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.OutputTag;

import javax.annotation.Nullable;
import java.util.Date;
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

            // Important! Set stream time characteristic to allow for watermarked streams
            streamEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
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

            DataStream<BrowserEvent> browserEventObject = browserEventString
                    .map(
                    new MapFunction<String, BrowserEvent>() {
                        @Override
                        public BrowserEvent map(String bStr) throws Exception {
                            System.out.println("* Received new event: " + bStr);
                            return new BrowserEvent(bStr);
                        }
                    }
            );

            /****************************************************************************
             * Perform stateful operations to print event durations
             ****************************************************************************/

            DataStream<Tuple3<String, String, Long>> browserEventDurations = browserEventObject
                    .map(i -> new Tuple3<String, String, Long>(
                            i.getUser(),
                            i.getAction(),
                            i.getTimestamp()
                    ))
                    .returns(Types.TUPLE(Types.STRING, Types.STRING, Types.LONG))
                    .keyBy(0)
                    .map(new RichMapFunction<Tuple3<String, String, Long>, Tuple3<String, String, Long>>() {

                        public transient ValueState<Long> lastTimestamp;
                        public transient ValueState<String> lastAction;

                        @Override
                        public void open(Configuration config) throws Exception {
                            ValueStateDescriptor<Long> timestampDescriptor = new ValueStateDescriptor<Long>(
                                    "last-timestamp",
                                    TypeInformation.of(new TypeHint<Long>() {})
                            );
                            lastTimestamp = getRuntimeContext().getState(timestampDescriptor);

                            ValueStateDescriptor<String> actionDescriptor = new ValueStateDescriptor<String>(
                                    "last-action",
                                    TypeInformation.of(new TypeHint<String>() {})
                            );
                            lastAction = getRuntimeContext().getState(actionDescriptor);
                        }

                        @Override
                        public Tuple3<String, String, Long> map(Tuple3<String, String, Long> browserTuple) throws Exception {
                            Tuple3<String, String, Long> browserEventDuration = browserTuple;
                            //System.out.println(lastTimestamp.value());
                            if (lastTimestamp.value() != null) {
                                Long currentTimestamp = browserTuple.f2;
                                Long duration = currentTimestamp - lastTimestamp.value();
                                browserEventDuration.f2 = duration;
                                browserEventDuration.f1 = lastAction.value();
                                System.out.println("[^] Browser Event:"
                                        + " User: " + browserEventDuration.f0
                                        + ", Action: " + browserEventDuration.f1
                                        + ", Duration: " + browserEventDuration.f2
                                );
                            }
                            lastAction.update(browserTuple.f1);
                            lastTimestamp.update(browserTuple.f2);


                            return browserEventDuration;
                        }
                    });


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

            // Create output tag for late records
            final OutputTag<Tuple4<String, String, Long, Integer>> lateBrowserTrail =
                    new OutputTag<Tuple4<String, String, Long, Integer>>("late-events"){};

            // Process the watermarked stream
            SingleOutputStreamOperator<Tuple4<String, String, Long, Integer>> browserEventProcessed = browserEventWatermarked
                    .map(i -> new Tuple4<String, String, Long, Integer>(
                            i.getUser(),
                            i.getAction(),
                            i.getTimestamp(),
                            1
                    ))
                    .returns(Types.TUPLE(Types.STRING, Types.STRING, Types.LONG, Types.INT))
                    .keyBy(0)
                    // Important! Use .timeWindow() instead of .timeWindowAll() for key-partitioned streams
                    .timeWindow(Time.seconds(10))
                    .sideOutputLateData(lateBrowserTrail)
                    // Aggregate counts by user
                    .reduce((x, y) ->
                        new Tuple4<String, String, Long, Integer>(
                                x.f0,
                                x.f1,
                                x.f2,
                                x.f3 + y.f3
                        )
                    )
                    .map(new MapFunction<Tuple4<String, String, Long, Integer>, Tuple4<String, String, Long, Integer>>() {
                        @Override
                        public Tuple4<String, String, Long, Integer> map(Tuple4<String, String, Long, Integer> minuteSummary) throws Exception {
                            String currentTime = (new Date()).toString();
                            String eventTime =
                                    (new Date(Long.valueOf(minuteSummary.f2))).toString();

                            System.out.println("Summary:"
                                    + " Current Time: " + currentTime
                                    + ", User: " + minuteSummary.f0
                                    + ", Action: " + minuteSummary.f1
                                    + ", Event Time: " + eventTime
                                    + ", Count : " + minuteSummary.f3
                            );

                            return minuteSummary;
                        }
                    });

//            browserEventProcessed.print();

            DataStream<Tuple4<String, String, Long, Integer>> lateEvents =
                    browserEventProcessed.getSideOutput(lateBrowserTrail);

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
