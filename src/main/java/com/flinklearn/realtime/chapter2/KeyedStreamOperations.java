package com.flinklearn.realtime.chapter2;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.api.java.tuple.Tuple2;

import java.io.File;

public class KeyedStreamOperations {

    public static void main(String[] args) {

        try {

            // Get stream env and print task slots
            final StreamExecutionEnvironment streamEnv =
                    StreamExecutionEnvironment.getExecutionEnvironment();
            System.out.println("\nTotal Parallel Task Slots:" + streamEnv.getParallelism());

            // Create data stream from file directory
            String dataDir = "data/raw_audit_trail";
            TextInputFormat auditFormat = new TextInputFormat(
                    new Path(dataDir)
            );
            DataStream<String> auditStream =
                    streamEnv.readFile(
                            auditFormat,
                            dataDir,
                            FileProcessingMode.PROCESS_CONTINUOUSLY,
                            1000
                    );

            // Key by user
            DataStream<Tuple2<String, Integer>> userCountStream = auditStream
                    .map(new MapFunction<String, Tuple2<String, Integer>>() {
                             @Override
                             public Tuple2<String, Integer> map(String auditStr) throws Exception {
                                 System.out.println("--- Received Record:" + auditStr);
                                 AuditTrail atObj = new AuditTrail(auditStr);
                                 return new Tuple2<String, Integer>(atObj.user, atObj.duration);
                             }
                         }
                    )
                    .keyBy(0)
                    .reduce((x, y) ->
                        new Tuple2<String, Integer>(x.f0, x.f1 + y.f1)
                    );

            userCountStream.print();

            // Set up file stream and execute environment
            Utils.printHeader("Starting file stream data generator");
            Thread genThread = new Thread(new FileStreamDataGenerator());
            genThread.start();

            streamEnv.execute("Flink keyed streams demo");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
