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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


/**
 * A flink pipeline which consumes json strings from a secure kafka source
 * parses them, applies transformations, and posts them to a secure elastic sink
 */

public class GitHubElasticSink {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Enable checkpointing every 500 messages
        env.enableCheckpointing(500);

        // Get config
        ReadProps readProps = new ReadProps();
        Properties configProps = readProps.load();

        // Begin reading from Kafka
        DataStream<ObjectNode> pullRequestStream = readFromKafka(env, configProps,"pr-test");
        DataStream<ObjectNode> issueStream = readFromKafka(env, configProps, "issue-test");

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
        writeToElastic(githubStream, configProps);

        // Start github data generator
//        Utils.printHeader("Starting github API data generator...");
//        Thread githubData = new Thread(new GitHubDataGenerator());
//        githubData.start();

        // Execute pipeline
        env.execute("Kafka to Elasticsearch!");
    }

    public static DataStream<ObjectNode> readFromKafka(StreamExecutionEnvironment env, Properties config, String topic) {

        // Extract kafka config
        String kafkaURL = String.valueOf(config.getProperty("kafka.host")) + ":" + String.valueOf(config.getProperty("kafka.port"));
        String securityProtocol = String.valueOf(config.getProperty("kafka.security.protocol"));
        String sslTruststorePassword = String.valueOf(config.getProperty("kafka.ssl.truststore.password"));
        String sslTruststoreLocation = String.valueOf(config.getProperty("kafka.ssl.truststore.location"));

        // Set properties for secure Kafka
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", kafkaURL);
        properties.setProperty("group.id", "consumer.group.1");
        properties.setProperty("security.protocol", securityProtocol);
        properties.setProperty("ssl.truststore.password", sslTruststorePassword);
        properties.setProperty("ssl.truststore.location", sslTruststoreLocation);

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

    public static void writeToElastic(DataStream<ObjectNode> input, Properties config) {

        try {

            // Add secure elasticsearch hosts on startup
            List<HttpHost> httpHosts = new ArrayList<>();
            httpHosts.add(new HttpHost(
                    String.valueOf(config.getProperty("elastic.host")),
                    Integer.valueOf(config.getProperty("elastic.port")),
                    "https"
            ));

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
                                    .index("pr-test")
                                    .id(id)
                                    .source(esJson);
                            System.out.println("Creating request: " + esJson.toString());
                        } else if(type.equals("filechange")) {
                            request
                                    .index("filechange")
                                    .id(id)
                                    .source(esJson);
                            System.out.println("Creating request: " + esJson.toString());
                        } else if(type.equals("comment")) {
                            request
                                    .index("comment")
                                    .id(id)
                                    .source(esJson);
                            System.out.println("Creating request: " + esJson.toString());
                        } else {
                            // Discard
                            System.out.println("Bad data, discarding: " + esJson.toString());
                            request.id("discard");
                        }
                    } else if(topic.equals("issue")) {
                        if(type.equals("issue")) {
                            request
                                    .index("issue-test")
                                    .id(id)
                                    .source(esJson);
                            System.out.println("Creating request: " + esJson.toString());
                        } else if(type.equals("comment")) {
                            request
                                    .index("comment")
                                    .id(id)
                                    .source(esJson);
                            System.out.println("Creating request: " + esJson.toString());
                        } else {
                            // Discard
                            System.out.println("Bad data, discarding: " + esJson.toString());
                            request.id("discard");
                        }
                    } else {
                        // Discard
                        System.out.println("Bad data, discarding: " + esJson.toString());
                        request.id("discard");
                    }

                    return request;

                }

                @Override
                public void process(ObjectNode element, RuntimeContext ctx, RequestIndexer indexer) {
                    try {
                        IndexRequest req = createIndexRequest(element);
                        if (req.id() != "discard") {
                            indexer.add(req);
                        }
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

            // provide a RestClientFactory for custom configuration on the internally created REST client
            esSinkBuilder.setRestClientFactory(
                    restClientBuilder -> {
                        restClientBuilder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {

                                // Get the elastic username and password from config
                                String elasticUser = String.valueOf(config.getProperty("elastic.user"));
                                String elasticPassword = String.valueOf(config.getProperty("elastic.password"));

                                // Set credentials
                                if (elasticUser != null && elasticPassword != null) {
                                    // elasticsearch username and password
                                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                                    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                                            elasticUser,
                                            elasticPassword
                                    ));
                                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                                }

                                try {
                                    // Trust self-signed certificates
                                    SSLContextBuilder builder = new SSLContextBuilder();
                                    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                                    SSLContext sslContext = builder.build();
                                    httpClientBuilder.setSSLContext(sslContext);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (KeyStoreException e) {
                                    e.printStackTrace();
                                } catch (KeyManagementException e) {
                                    e.printStackTrace();
                                }

                                return httpClientBuilder;
                            }
                        });
                    }
            );

            // Add elastic sink to input stream
            input.addSink(esSinkBuilder.build());
            System.out.println("Built elasticsearch sink and added to stream");

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}