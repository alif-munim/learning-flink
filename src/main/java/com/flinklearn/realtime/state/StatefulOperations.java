package com.flinklearn.realtime.state;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import com.flinklearn.realtime.datastreamapi.AuditTrail;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;

import java.util.stream.Stream;

public class StatefulOperations {

    public static void main(String[] args) {
        try {

            /*******************************************************************
             * Set up stream execution environment
             *******************************************************************/

            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();
            streamEnv.setParallelism(1);

            /*******************************************************************
             * Create file data stream
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

            /*******************************************************************
             * Convert to audit obj stream and extract required state
             *******************************************************************/

            DataStream<Tuple3<String, String, Long>> auditObject =
                    auditString.map(
                            new MapFunction<String, Tuple3<String, String, Long>>() {
                                @Override
                                public Tuple3<String, String, Long> map(String atStr) throws Exception {
                                    AuditTrail atObj = new AuditTrail(atStr);
                                    return new Tuple3<String, String, Long>(
                                        atObj.getUser(),
                                        atObj.getOperation(),
                                        atObj.getTimestamp()
                                    );
                                }
                            }
                    );

            /*******************************************************************
             * Measure time between delete operations
             *******************************************************************/

            DataStream<Tuple2<String, Long>> deleteIntervals = auditObject
                    .keyBy(0)
                    .map(new RichMapFunction<Tuple3<String, String, Long>, Tuple2<String, Long>>() {

                        private transient ValueState<Long> lastDelete;

                        @Override
                        public void open(Configuration config) throws Exception {
                            ValueStateDescriptor<Long> descriptor =
                                    new ValueStateDescriptor<Long>(
                                            "last-delete",
                                            TypeInformation.of(new TypeHint<Long>() {})
                                    );
                            lastDelete = getRuntimeContext().getState(descriptor);
                        }

                        @Override
                        public Tuple2<String, Long> map(Tuple3<String, String, Long> auditObj) throws Exception {
                            Tuple2<String, Long> alertTuple = new Tuple2<String, Long>("No-Alerts", 0L);
                            if(auditObj.f1.equals("Delete")) {
                                if(lastDelete.value() != null) {
                                    long timeDifference = auditObj.f2 - lastDelete.value();
                                    if(timeDifference < 10000L) {
                                        alertTuple = new Tuple2<String, Long>(
                                                auditObj.f0,
                                                timeDifference
                                        );
                                    }
                                }
                                lastDelete.update(auditObj.f2);
                            }
                            return alertTuple;
                        }
                    })
                    .filter(new FilterFunction<Tuple2<String, Long>>() {
                        @Override
                        public boolean filter(Tuple2<String, Long> alert) throws Exception {
                            if(alert.f0.equals("No-Alerts")) {
                                return false;
                            } else {
                                System.out.println("\n[!] DELETE Alert Received: User " +
                                        alert.f0 + " executed 2 deletes within " +
                                        alert.f1 + "ms" + "\n");
                                return true;
                            }
                        }
                    });


            /*******************************************************************
             * Start file streaming thread and execute pipeline
             *******************************************************************/

            Utils.printHeader("Starting File Data Generator...");
            Thread genThread = new Thread(new FileStreamDataGenerator());
            genThread.start();

            streamEnv.execute("Flink Streaming Stateful Operations Example");

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
