package com.flinklearn.realtime.kafkaelastic;

import com.flinklearn.realtime.common.Utils;
import com.flinklearn.realtime.datasource.IPDataGenerator;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;

import java.net.InetSocketAddress;
import java.util.*;


/**
 * A flink pipeline with a kafka source and elasticsearch sink
 * Original code by Keira Zhou on 05/10/2016
 * See the original source code at the link below
 * https://github.com/keiraqz/KafkaFlinkElastic/blob/master/src/main/java/viper/KafkaFlinkElastic.java
 * Revised for elasticsearch 7 by Alif Munim on 04/13/2020
 */

public class KafkaFlinkElastic {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> stream = readFromKafka(env);
        stream.print();
        writeToElastic(stream);
        // Start ip data generator
        Utils.printHeader("Starting ip data generator...");
        Thread ipData = new Thread(new IPDataGenerator());
        ipData.start();
        // execute program
        env.execute("Kafka to Elasticsearch!");
    }

    public static DataStream<String> readFromKafka(StreamExecutionEnvironment env) {
        env.enableCheckpointing(5000);
        // set up the execution environment
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "test");

        DataStream<String> stream = env.addSource(
                new FlinkKafkaConsumer<>("ip.info", new SimpleStringSchema(), properties));
        return stream;
    }

    public static void writeToElastic(DataStream<String> input) {

        Map<String, String> config = new HashMap<>();

        try {
            // Add elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost("127.0.0.1", 9200, "http"));
            httpHosts.add(new HttpHost("10.2.3.1", 9200, "http"));

            ElasticsearchSinkFunction<String> indexLog = new ElasticsearchSinkFunction<String>() {
                public IndexRequest createIndexRequest(String element) {
                    String[] logContent = element.trim().split(",");
                    Map<String, String> esJson = new HashMap<>();
                    esJson.put("IP", logContent[0]);
                    esJson.put("info", logContent[1]);

                    return Requests
                            .indexRequest()
                            .index("ip-test")
                            .source(esJson);
                }

                @Override
                public void process(String element, RuntimeContext ctx, RequestIndexer indexer) {
                    indexer.add(createIndexRequest(element));
                }
            };

            ElasticsearchSink.Builder<String> esSinkBuilder = new ElasticsearchSink.Builder<String>(httpHosts, indexLog);
            esSinkBuilder.setBulkFlushMaxActions(25);
            input.addSink(esSinkBuilder.build());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}