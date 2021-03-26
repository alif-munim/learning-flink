package com.flinklearn.realtime.chapter2;

import com.flinklearn.realtime.common.MapCountPrinter;
import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.lang.reflect.Type;
import java.io.File;
import org.apache.flink.core.fs.Path;


public class BasicStreamingOperations {
    public static void main(String[] args) {
        try {

            /**************************************************************
             *                 Setup Flink env.
             **************************************************************/

            // Set up the stream execution environment
            final StreamExecutionEnvironment streamEnv
                    = StreamExecutionEnvironment.getExecutionEnvironment();

            // Set parallelism to 1 to maintain order of records
            streamEnv.setParallelism(1);

            /**************************************************************
             *          Read CSV File Stream into DataStream.
             **************************************************************/

            // Define data directory for monitoring
            String dataDir = "data/raw_audit_trail";

            // Define text input format based on directory
            TextInputFormat auditFormat = new TextInputFormat(
                    new org.apache.flink.core.fs.Path(dataDir)
            );

            // Read files from data directory into String DataStream
            DataStream<String> auditTrailStr = streamEnv.readFile(
                    auditFormat,
                    dataDir,
                    FileProcessingMode.PROCESS_CONTINUOUSLY,
                    1000
            );

            // Map auditTrailStr DataStream to AuditTrail objects
            DataStream<AuditTrail> auditTrailObj = auditTrailStr
                    .map(new MapFunction<String, AuditTrail>() {
                        @Override
                        public AuditTrail map(String auditStr) throws Exception {
                            System.out.println("--- Received Record: " + auditStr);
                            return new AuditTrail(auditStr);
                        }
                    });

            /**************************************************************
             *          Perform computations and write to output sink.
             **************************************************************/

            MapCountPrinter.printCount(
                    auditTrailObj.map(i -> (Object)i),
                    "Audit Trail: Last 5 secs"
            );

            DataStream<Tuple2<String, Integer>> recCount = auditTrailObj.map(
                    i -> new Tuple2<String, Integer>
                            (String.valueOf(System.currentTimeMillis()), 1))
                            .returns(Types.TUPLE(Types.STRING, Types.INT))
                            .timeWindowAll(Time.seconds(5))
                            .reduce((x, y) ->
                                    (new Tuple2<String, Integer>(x.f0, x.f1 + y.f1))
            );

            // Define output directory to store summary info
            String outputDir = "data/five_sec_summary";

            // Clean out existing files
            FileUtils.cleanDirectory(new File(outputDir));

            // Set up a streaming file sink to output directory
            final StreamingFileSink<Tuple2<String, Integer>> countSink
                    = StreamingFileSink
                    .forRowFormat(new Path(outputDir),
                            new SimpleStringEncoder<Tuple2<String, Integer>>
                                    ("UTF-8"))
                    .build();

            // Add the file sink as sink to the DataStream
            recCount.addSink(countSink);

            /**************************************************************
             *          Perform computations and write to output sink.
             **************************************************************/

            // Start file stream generator on separate thread
            Utils.printHeader("Starting File Data Generator");
            Thread genThread = new Thread(new FileStreamDataGenerator());
            genThread.start();

            // Execute the streaming pipeline
            streamEnv.execute("Flink streaming audit trail");


        } catch(Exception e) {

        }
    }
}
