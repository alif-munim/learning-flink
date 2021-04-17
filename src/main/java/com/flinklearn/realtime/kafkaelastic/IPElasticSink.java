package com.flinklearn.realtime.kafkaelastic;

import com.flinklearn.realtime.common.Utils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.util.ExceptionUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;

import java.util.*;


/**
 * A flink pipeline with a kafka source and elasticsearch sink
 * Original code by Keira Zhou on 05/10/2016
 * See the original source code at the link below
 * https://github.com/keiraqz/KafkaFlinkElastic/blob/master/src/main/java/viper/KafkaFlinkElastic.java
 * Revised for elasticsearch 7 by Alif Munim on 04/13/2020
 */

public class IPElasticSink {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing every 500 messages
        env.enableCheckpointing(500);

        // Begin reading from Kafka
        DataStream<String> stream = readFromKafka(env);
        stream.print();

        // Perform operations and write stream to elastic
        writeToElastic(stream);

        // Start ip data generator
        Utils.printHeader("Starting ip data generator...");
        Thread ipData = new Thread(new IPDataGenerator());
        ipData.start();

        // execute program
        env.execute("Kafka to Elasticsearch!");
    }

    public static DataStream<String> readFromKafka(StreamExecutionEnvironment env) {

        // Set properties for Kafka
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "test");

        // Add kafka as streaming source
        DataStream<String> stream = env.addSource(
                new FlinkKafkaConsumer<>("ip.info", new SimpleStringSchema(), properties));
        return stream;
    }

    public static void writeToElastic(DataStream<String> input) {

        try {
            // Add elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost("127.0.0.1", 9200, "http"));
            httpHosts.add(new HttpHost("10.2.3.1", 9200, "http"));

            // Create indexing function
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

            // Create failure handler function
            ActionRequestFailureHandler failureHandler = new ActionRequestFailureHandler() {
                @Override
                public void onFailure(ActionRequest action,
                               Throwable failure,
                               int restStatusCode,
                               RequestIndexer indexer) throws Throwable {

                    if (ExceptionUtils.findThrowable(failure, EsRejectedExecutionException.class).isPresent()) {
                        // full queue; re-add document for indexing
                        indexer.add(action);
                    } else if (ExceptionUtils.findThrowable(failure, ElasticsearchParseException.class).isPresent()) {
                        // malformed document; simply drop request without failing sink
                    } else {
                        // for all other failures, fail the sink
                        // here the failure is simply rethrown, but users can also choose to throw custom exceptions
                        throw failure;
                    }
                }
            };

            // Create sink builder
            ElasticsearchSink.Builder<String> esSinkBuilder = new ElasticsearchSink.Builder<String>(httpHosts, indexLog);

            // Set config options
            esSinkBuilder.setBulkFlushMaxActions(25);
            esSinkBuilder.setFailureHandler(failureHandler);
            esSinkBuilder.setBulkFlushBackoffRetries(1);

            // Add elastic sink to input stream
            input.addSink(esSinkBuilder.build());
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}