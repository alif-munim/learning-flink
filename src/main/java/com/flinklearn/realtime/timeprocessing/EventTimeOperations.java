package com.flinklearn.realtime.timeprocessing;

import com.flinklearn.realtime.datastreamapi.AuditTrail;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;

import javax.annotation.Nullable;
import java.util.Date;

public class EventTimeOperations {

    public static void main(String[] args) {

        /*******************************************************************
         * Set up stream execution environment
         *******************************************************************/
        final StreamExecutionEnvironment streamEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        streamEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        streamEnv.setParallelism(1);

        /*******************************************************************
         * Set up basic pipeline
         *******************************************************************/

        String dataDir = "data/raw_audit_trail";
        TextInputFormat auditFormat = new TextInputFormat(
                new Path(dataDir)
        );
        DataStream<String> auditString = streamEnv.readFile(
                auditFormat,
                dataDir,
                FileProcessingMode.PROCESS_CONTINUOUSLY,
                1000
        );

        DataStream<AuditTrail> auditObject = auditString.map(
                new MapFunction<String, AuditTrail>() {
                    @Override
                    public AuditTrail map(String atStr) throws Exception {
                        System.out.println("* Received record: " + atStr);
                        return new AuditTrail(atStr);
                    }
                }
        );

        /*******************************************************************
         * Set up event time and watermarked data stream
         *******************************************************************/

        DataStream<AuditTrail> auditMarked = auditObject.assignTimestampsAndWatermarks(
                new AssignerWithPunctuatedWatermarks<AuditTrail>() {

                    transient long currentWatermark = 0L;
                    int delay = 10000;
                    int buffer = 2000;

                    @Nullable
                    @Override
                    public Watermark checkAndGetNextWatermark(AuditTrail auditTrail, long nextTimestamp) {

                        long currentTime = System.currentTimeMillis();

                        if(currentWatermark == 0L) {

                        } else if(currentTime - currentWatermark > delay) {
                            currentWatermark = currentTime;
                        }

                        return new Watermark(currentWatermark - buffer);

                    }

                    @Override
                    public long extractTimestamp(AuditTrail auditTrail, long previousTimestamp) {
                        return auditTrail.getTimestamp();
                    }
                }
        );

        /*******************************************************************
         * Process watermarked stream
         *******************************************************************/

        // Create output tag for late records
        final OutputTag<Tuple2<String, Integer>> lateAuditTrail =
                new OutputTag<Tuple2<String, Integer>>("late-audit-trail");

        // Use single output stream operator to produce side output
        SingleOutputStreamOperator<Tuple2<String, Integer>> finalTrail =
                auditMarked
                .map(i -> new Tuple2<String, Integer>
                        (String.valueOf(i.getTimestamp()), 1)
                )
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .timeWindowAll(Time.seconds(1))
                .sideOutputLateData(lateAuditTrail)
                .reduce((x, y) ->
                        (new Tuple2<String, Integer>(x.f0, x.f1 + y.f1))
                )
                .map(
                        new MapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {
                            @Override
                            public Tuple2<String, Integer> map(Tuple2<String, Integer> minuteSummary) throws Exception {
                                String currentTime = (new Date()).toString();
                                String eventTime =
                                        (new Date(Long.valueOf(minuteSummary.f0))).toString();

                                System.out.println("Summary:"
                                        + " Current Time: " + currentTime
                                        + " Event Time: " + eventTime
                                        + " Count : " + minuteSummary.f1
                                );

                                return minuteSummary;
                            }
                        }
                );

        // Collect late events for later processing
        DataStream<Tuple2<String, Integer>> lateEvents =
                finalTrail.getSideOutput(lateAuditTrail);
    }

}
