package com.flinklearn.realtime.githubkafkaelastic;

import com.flinklearn.realtime.common.Utils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


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
        DataStream<ObjectNode> pullRequestStream = readFromKafka(env, "pullrequest");
        DataStream<ObjectNode> issueStream = readFromKafka(env, "issue");

        // Connect streams
        ConnectedStreams<ObjectNode, ObjectNode> githubConnected = pullRequestStream.connect(issueStream);
        DataStream<ObjectNode> githubStream = githubConnected.map(
                new CoMapFunction<ObjectNode, ObjectNode, ObjectNode>() {
                    @Override
                    public ObjectNode map1(ObjectNode obj) throws Exception {
                        ObjectNode newObj = ((ObjectNode)obj.get("value")).put("topic", "pullrequest");
                        return newObj;
                    }

                    @Override
                    public ObjectNode map2(ObjectNode obj) throws Exception {
                        ObjectNode newObj = ((ObjectNode)obj.get("value")).put("topic", "issue");
                        return newObj;
                    }
                }
        );

        // Print stream
        githubStream.print();

        // Add elastic sink to source
        writeToElastic(githubStream);

        // Start ip data generator
        Utils.printHeader("Starting ip data generator...");
        Thread githubData = new Thread(new GitHubDataGenerator());
        githubData.start();

        // Execute pipeline
        env.execute("Kafka to Elasticsearch!");
    }

    public static DataStream<ObjectNode> readFromKafka(StreamExecutionEnvironment env, String topic) {

        // Set properties for Kafka
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        properties.setProperty("group.id", "test");

        // Add kafka as streaming source
        DataStream<ObjectNode> stream = env.addSource(
                new FlinkKafkaConsumer<>(topic, new JSONKeyValueDeserializationSchema(false), properties));
        return stream;
    }

    public static Map pullRequestMapping(JsonNode element) {
        String id = element.get("id").asText();
        String type = element.get("type").asText();
        String user = element.get("user").asText();
        String branch = element.get("branch").asText();

        Map<String, String> esJson = new HashMap<>();
        esJson.put("id", id);
        esJson.put("type", type);
        esJson.put("user", user);
        esJson.put("branch", branch);

        return esJson;
    }

    public static void writeToElastic(DataStream<ObjectNode> input) {

        try {

            // Add elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost("127.0.0.1", 9200, "http"));
            httpHosts.add(new HttpHost("10.2.3.1", 9200, "http"));

            // Create indexing function
            ElasticsearchSinkFunction<ObjectNode> indexLog = new ElasticsearchSinkFunction<ObjectNode>() {

                public IndexRequest createIndexRequest(ObjectNode element) throws IOException {

                    // Pass element to mapping function and get type
                    String topic = element.get("topic").asText();
                    Map<String, String> esJson = pullRequestMapping(element);
                    String type = esJson.get("type");
                    String id = esJson.get("id");

                    // Create an empty index request
                    IndexRequest request = Requests.indexRequest();

                    // Choose index
                    if(topic.equals("pullrequest")) {
                        if(type.equals("pullrequest")) {
                            request
                                    .index("pullrequest")
                                    .id(id)
                                    .source(esJson);
                        } else if(type.equals("filechange")) {
                            request
                                    .index("filechange")
                                    .id(id)
                                    .source(esJson);
                        } else if(type.equals("comment")) {
                            request
                                    .index("comment")
                                    .id(id)
                                    .source(esJson);
                        } else {
                            // Discard
                        }
                    } else if(topic.equals("issue")) {
                        if(type.equals("issue")) {
                            request
                                    .index("issue")
                                    .id(id)
                                    .source(esJson);
                        } else if(type.equals("comment")) {
                            request
                                    .index("comment")
                                    .id(id)
                                    .source(esJson);
                        } else {
                            // Discard
                        }
                    } else {
                        // Discard
                    }

                    return request;

                }

                @Override
                public void process(ObjectNode element, RuntimeContext ctx, RequestIndexer indexer) {
                    try {
                        indexer.add(createIndexRequest(element));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            // Create sink builder
            ElasticsearchSink.Builder<ObjectNode> esSinkBuilder = new ElasticsearchSink.Builder<ObjectNode>(httpHosts, indexLog);

            // Set config options
            esSinkBuilder.setBulkFlushMaxActions(50);
            esSinkBuilder.setBulkFlushBackoffRetries(1);

            // Add elastic sink to input stream
            input.addSink(esSinkBuilder.build());

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}