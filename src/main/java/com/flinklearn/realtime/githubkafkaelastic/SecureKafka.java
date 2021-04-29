package com.flinklearn.realtime.githubkafkaelastic;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import java.util.HashMap;
import java.util.Properties;

public class SecureKafka {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing every 500 messages
        env.enableCheckpointing(500);

        // Get kafka config
        ReadProps readProps = new ReadProps();
        Properties kafkaProp = readProps.load();

        // Begin reading from Kafka
        DataStream<String> readStream = readFromKafka(env, kafkaProp, "alif-test");

        // Print stream
        readStream.print();

        // Execute pipeline
        env.execute("Secure Kafka!");
    }

    public static DataStream<String> readFromKafka(StreamExecutionEnvironment env, Properties config, String topic) {

        // Extract kafka config
        String kafkaURL = String.valueOf(config.getProperty("kafka.host")) + ":" + String.valueOf(config.getProperty("kafka.port"));
        String securityProtocol = String.valueOf(config.getProperty("kafka.security.protocol"));
        String sslTruststorePassword = String.valueOf(config.getProperty("kafka.ssl.truststore.password"));
        String sslTruststoreLocation = String.valueOf(config.getProperty("kafka.ssl.truststore.location"));

        // Set properties for Kafka
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", kafkaURL);
        properties.setProperty("group.id", "consumer.group.1");
        properties.setProperty("security.protocol", securityProtocol);
        properties.setProperty("ssl.truststore.password", sslTruststorePassword);
        properties.setProperty("ssl.truststore.location", sslTruststoreLocation);


        // Add kafka as streaming source
        DataStream<String> stream = env.addSource(
                new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), properties));
        return stream;
    }
}
