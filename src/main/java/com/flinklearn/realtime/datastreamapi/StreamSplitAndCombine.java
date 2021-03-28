package com.flinklearn.realtime.datastreamapi;

import com.flinklearn.realtime.common.MapCountPrinter;
import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;

public class StreamSplitAndCombine {

    public static void main(String[] args) {

        try {

            /********************************************************************
             * Set up environment
             *******************************************************************/
            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();

            /********************************************************************
             * Read CSV file into data stream
             *******************************************************************/

            String dataDir = "data/raw_audit_trail";
            TextInputFormat auditFormat = new TextInputFormat(
                    new Path(dataDir)
            );
            DataStream<String> auditStream = streamEnv.readFile(
                    auditFormat,
                    dataDir,
                    FileProcessingMode.PROCESS_CONTINUOUSLY,
                    1000
            );

            /********************************************************************
             * Split streams based on entity
             *******************************************************************/

            final OutputTag<Tuple2<String, Integer>> salesRepTag =
                    new OutputTag<Tuple2<String, Integer>>("sales-rep"){};

            SingleOutputStreamOperator<AuditTrail> customerStream
                    = auditStream.process(new ProcessFunction<String, AuditTrail>() {
                    @Override
                    public void processElement(String auditStr, Context context, Collector<AuditTrail> collector)
                            throws Exception {
                        System.out.println("--- Received Record: " + auditStr);
                        AuditTrail auditTrailObj = new AuditTrail(auditStr);
                        Tuple2<String, Integer> entityCount =
                                new Tuple2<String, Integer>(auditTrailObj.user, 1);

                        /* Collect customer objects in this stream,
                        *  pipe sales rep objects to context with sales rep tag
                        * */
                        if(auditTrailObj.getEntity().equals("Customer")) {
                            collector.collect(auditTrailObj);
                        } else {
                            context.output(salesRepTag, entityCount);
                        }
                    }
            });

            // Convert side output into data stream
            DataStream<Tuple2<String, Integer>> salesRepStream =
                    customerStream.getSideOutput(salesRepTag);

            // Print record summaries
            MapCountPrinter.printCount(
                    customerStream.map(i -> (Object)i),
                    "Customer records in stream: Last 5 secs"
            );

            MapCountPrinter.printCount(
                    salesRepStream.map(i -> (Object)i),
                    "Sales rep records in stream: Last 5 secs"
            );

            /********************************************************************
             * Combine the two streams into one
             *******************************************************************/
            ConnectedStreams<AuditTrail, Tuple2<String, Integer>> mergedTrail =
                    customerStream.connect(salesRepStream);

            DataStream<Tuple3<String, String, Integer>> processedTrail =
                    mergedTrail.map(new CoMapFunction<AuditTrail, Tuple2<String, Integer>, Tuple3<String, String, Integer>>() {
                        @Override
                        public Tuple3<String, String, Integer> map1(AuditTrail cTrail) throws Exception {
                            return new Tuple3<String, String, Integer>(
                                "Stream-1", cTrail.user, 1
                            );
                        }

                        @Override
                        public Tuple3<String, String, Integer> map2(Tuple2<String, Integer> srTrail) throws Exception {
                            return new Tuple3<String, String, Integer>(
                                "Stream-2", srTrail.f0, 1
                            );
                        }
                    });

            /********************************************************************
             * Print merged records
             *******************************************************************/
            processedTrail.map(new MapFunction<Tuple3<String, String, Integer>, Tuple3<String, String, Integer>>() {
                @Override
                public Tuple3<String, String, Integer> map(Tuple3<String, String, Integer> user) {
                    System.out.println("--- Merged record for user: " + user);
                    return null;
                }
            });

            /********************************************************************
             * Configure data source and execute
             *******************************************************************/
            Utils.printHeader("Starting file stream data generator");
            Thread genThread = new Thread(new FileStreamDataGenerator());
            genThread.start();

            streamEnv.execute("Starting stream splitting demo");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
