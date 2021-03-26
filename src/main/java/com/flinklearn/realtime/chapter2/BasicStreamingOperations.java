package com.flinklearn.realtime.chapter2;

import com.flinklearn.realtime.datasource.FileStreamDataGenerator;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;


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

        } catch(Exception e) {

        }
    }
}
