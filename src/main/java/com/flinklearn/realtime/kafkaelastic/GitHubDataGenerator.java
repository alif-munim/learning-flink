package com.flinklearn.realtime.kafkaelastic;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.types.Record;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import javax.xml.ws.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class GitHubDataGenerator implements Runnable {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) {
        GitHubDataGenerator ghdg = new GitHubDataGenerator();
        ghdg.run();
    }

    public void run() {
        try {

            // Set kafka producer properties and create producer
            Properties properties = new Properties();
            properties.put("bootstrap.servers", "localhost:9092");
            properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            Producer<String, String> myProducer = new KafkaProducer<String, String>(properties);

            // Create lists to randomly select from
            List<String> type = new ArrayList<>();
            type.add("pullrequest");
            type.add("filechange");
            type.add("comment");

            List<String> user = new ArrayList<>();
            user.add("t-stark");
            user.add("steverogers");
            user.add("brucebann");
            user.add("jarvis");

            List<String> branch = new ArrayList<>();
            branch.add("master");
            branch.add("ultron");
            branch.add("friday");
            branch.add("karen");

            Random random = new Random();

            for(int i = 0; i < 5000; i++) {

                // Create random json object
                String thisType = type.get(random.nextInt(type.size()));
                String thisUser = user.get(random.nextInt(user.size()));
                String thisBranch = branch.get(random.nextInt(branch.size()));

                String jsonData = "{\"type\": \"" + thisType + "\", " +
                        "\"user\": \"" + thisUser + "\", " +
                        "\"branch\": \"" + thisBranch + "\"}";

                // Create key and producer record
                String currentTime = String.valueOf(System.currentTimeMillis());
                ProducerRecord<String, String> myRecord = new ProducerRecord<String, String>(
                    "github.data",
                        currentTime,
                        jsonData
                );

                // Send record and print
                RecordMetadata rmd = myProducer.send(myRecord).get();
                System.out.println(ANSI_PURPLE + "GitHub API, sending response: "
                        + jsonData  + ANSI_RESET);

                Thread.sleep(5);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
