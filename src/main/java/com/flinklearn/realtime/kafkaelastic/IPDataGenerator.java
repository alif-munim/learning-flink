package com.flinklearn.realtime.kafkaelastic;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class IPDataGenerator implements Runnable {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) {
        IPDataGenerator ipdg = new IPDataGenerator();
        ipdg.run();
    }

    @Override
    public void run() {

        try {

            // Set Kafka producer config properties
            Properties properties = new Properties();
            properties.put("bootstrap.servers", "localhost:9092");
            properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            // Create Kafka producer
            Producer<String, String> myProducer = new KafkaProducer<String, String>(properties);

            // Create lists for ip and users to randomly select from
            List<String> ipList = new ArrayList<>();
            ipList.add("185.77.248.70");
            ipList.add("13.66.139.159");
            ipList.add("13.66.139.3");
            ipList.add("171.116.59.244");
            ipList.add("191.96.67.230");

            List<String> connList = new ArrayList<>();
            connList.add("John Doe");
            connList.add("Jane Doe");
            connList.add("Bobby Smith");
            connList.add("Alex Ramirez");
            connList.add("Draco Malfoy");

            Random random = new Random();

            // Randomly generate 100 records
            for(int i = 0; i < 100; i++) {

                // Randomly select data
                String ip = ipList.get(random.nextInt(ipList.size()));
                String conn = connList.get(random.nextInt(connList.size()));

                String[] ipInfo = {
                        ip,
                        conn
                };

                String currentTime = String.valueOf(System.currentTimeMillis());

                // Create producer record with topic, key, value
                ProducerRecord<String, String> myRecord = new ProducerRecord<String, String>(
                        "ip.info",
                        currentTime,
                        String.join(",", ipInfo)
                );

                RecordMetadata recordMetadata = myProducer.send(myRecord).get();
                System.out.println(ANSI_PURPLE + "Kafka Stream Generator : Sending Event : "
                        + String.join(",", ipInfo)  + ANSI_RESET);
                Thread.sleep(500);

            }

        } catch(Exception e) {
            e.printStackTrace();
        }



    }
}
