package com.flinklearn.realtime.kafkaelastic;

import com.flinklearn.realtime.common.Utils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch7.ElasticsearchSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.JSONKeyValueDeserializationSchema;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.util.Collector;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.OutputTag;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import scala.collection.script.Update;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        DataStream<ObjectNode> stream = readFromKafka(env);

        // Add elastic sink to source
        writeToElastic(stream);

        // Start ip data generator
        Utils.printHeader("Starting ip data generator...");
        Thread githubData = new Thread(new GitHubDataGenerator());
        githubData.start();

        // Execute pipeline
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

    public static Map jsonMapping(JsonNode element) {
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

            DataStream<JsonNode> jsonData = input
                .map(new MapFunction<ObjectNode, JsonNode>() {
                    @Override
                    public JsonNode map(ObjectNode value) {
                        JsonNode object = value.get("value");
                        System.out.println(object.toString());
                        return object;
                    }
                });

            // Add elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost("127.0.0.1", 9200, "http"));
            httpHosts.add(new HttpHost("10.2.3.1", 9200, "http"));

            // Create indexing function
            ElasticsearchSinkFunction<JsonNode> indexLog = new ElasticsearchSinkFunction<JsonNode>() {

                public IndexRequest createIndexRequest(JsonNode element) throws IOException {

                    // Pass json element to mapping function and get type
                    Map<String, String> esJson = jsonMapping(element);
                    String type = esJson.get("type");
                    String id = esJson.get("id");

                    // Create an empty index request
                    IndexRequest request = Requests.indexRequest();

                    // Choose index
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
                    return request;

                }

                @Override
                public void process(JsonNode element, RuntimeContext ctx, RequestIndexer indexer) {
                    try {
                        indexer.add(createIndexRequest(element));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            // Create sink builder
            ElasticsearchSink.Builder<JsonNode> esSinkBuilder = new ElasticsearchSink.Builder<JsonNode>(httpHosts, indexLog);

            // Set config options
            esSinkBuilder.setBulkFlushMaxActions(100);
            esSinkBuilder.setBulkFlushBackoffRetries(1);

            // Add elastic sink to input stream
            jsonData.addSink(esSinkBuilder.build());

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}