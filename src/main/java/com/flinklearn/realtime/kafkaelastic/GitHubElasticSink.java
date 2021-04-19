package com.flinklearn.realtime.kafkaelastic;

import com.flinklearn.realtime.common.Utils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
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
 * A flink pipeline which consumes json strings from a kafka source
 * parses them, applies transformations, and posts them to an elastic sink
 */

public class GitHubElasticSink {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing every 500 messages
        env.enableCheckpointing(500);

        // Begin reading from Kafka
        DataStream<ObjectNode> stream = readFromKafka(env);
        stream.print();

        // Perform operations and write stream to elastic
        writeToElastic(stream);

        // Start ip data generator
        Utils.printHeader("Starting ip data generator...");
        Thread githubData = new Thread(new GitHubDataGenerator());
        githubData.start();

        // execute program
        env.execute("Kafka to Elasticsearch!");
    }

    public static DataStream<ObjectNode> readFromKafka(StreamExecutionEnvironment env) {

        // Set properties for Kafka
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "test");

        // Add kafka as streaming source
        DataStream<ObjectNode> stream = env.addSource(
                new FlinkKafkaConsumer<>("github.data", new JSONKeyValueDeserializationSchema(false), properties));
        return stream;
    }

    public static void writeToElastic(DataStream<ObjectNode> input) {

        try {

            DataStream<JsonNode> jsonData = input.map(new MapFunction<ObjectNode, JsonNode>() {
                @Override
                public JsonNode map(ObjectNode value) {
                    JsonNode object = value.get("value");
                    return object;
                }
            });

            // Add elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost("127.0.0.1", 9200, "http"));
            httpHosts.add(new HttpHost("10.2.3.1", 9200, "http"));

            // Create indexing function
            ElasticsearchSinkFunction<JsonNode> indexLog = new ElasticsearchSinkFunction<JsonNode>() {
                public IndexRequest createIndexRequest(JsonNode element) {

                    // String[] logContent = element.trim().split(",");
                    String type = element.get("type").asText();
                    String user = element.get("user").asText();
                    String branch = element.get("branch").asText();

                    Map<String, String> esJson = new HashMap<>();
                    esJson.put("type", type);
                    esJson.put("user", user);
                    esJson.put("branch", branch);

                    return Requests
                            .indexRequest()
                            .index("github")
                            .source(esJson);
                }

                @Override
                public void process(JsonNode element, RuntimeContext ctx, RequestIndexer indexer) {
                    indexer.add(createIndexRequest(element));
                }
            };


            // Create sink builder
            ElasticsearchSink.Builder<JsonNode> esSinkBuilder = new ElasticsearchSink.Builder<JsonNode>(httpHosts, indexLog);

            // Set config options
            esSinkBuilder.setBulkFlushMaxActions(25);
            esSinkBuilder.setBulkFlushBackoffRetries(1);

            // Add elastic sink to input stream
            jsonData.addSink(esSinkBuilder.build());

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}