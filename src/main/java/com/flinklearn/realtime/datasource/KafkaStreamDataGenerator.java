package com.flinklearn.realtime.datasource;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.*;

/**************************************************************************
 *  Publishes records to flink.kafka.streaming.source topic
 *  in Kafka
 **************************************************************************/

public class KafkaStreamDataGenerator implements Runnable {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) {
        KafkaStreamDataGenerator ksdg = new KafkaStreamDataGenerator();
        ksdg.run();
    }

    public void run() {
        try {

            // Set up Kafka client
            Properties kafkaProps = new Properties();
            kafkaProps.put("bootstrap.servers", "localhost:9092");
            kafkaProps.put("key.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");
            kafkaProps.put("value.serializer",
                    "org.apache.kafka.common.serialization.StringSerializer");

            // Create Kafka producer
            Producer<String, String> myProducer =
                    new KafkaProducer<String, String>(kafkaProps);

            // Define list of users
            List<String> appUser = new ArrayList<String>();
            appUser.add("Tom");
            appUser.add("Harry");
            appUser.add("Bob");

            // Define list of application operations
            List<String> appOperation = new ArrayList<String>();
            appOperation.add("Create");
            appOperation.add("Modify");
            appOperation.add("Query");
            appOperation.add("Delete");

            // Define list of application entities
            List<String> appEntity = new ArrayList<String>();
            appEntity.add("Customer");
            appEntity.add("SalesRep");

            // Define a random number generator
            Random random = new Random();

            // Generate 100 audit records and send stringified producer records
            for(int i=0; i < 100; i++) {
                // Pick random values
                String user = appUser.get(random.nextInt(appUser.size()));
                String operation = appOperation.get(random.nextInt(appOperation.size()));
                String entity = appEntity.get(random.nextInt(appEntity.size()));
                String duration = String.valueOf(random.nextInt(10) + 1);
                String changeCount = String.valueOf(random.nextInt(4) + 1);

                // Get current time
                String currentTime = String.valueOf(System.currentTimeMillis());

                // Create CSV array
                String[] csv = {
                        String.valueOf(i),
                        user,
                        entity,
                        operation,
                        duration,
                        changeCount,
                        currentTime
                };

                // Create key and producer record
                String recKey = String.valueOf(currentTime);
                ProducerRecord<String, String> record = new ProducerRecord(
                        "flink.kafka.streaming.source",
                        recKey,
                        String.join(",", csv)
                );

                // Send record
                RecordMetadata rmd = myProducer.send(record).get();

                System.out.println(ANSI_PURPLE + "Kafka Stream Generator : Sending Event : "
                        + String.join(",", csv)  + ANSI_RESET);

                // Sleep before next record
                Thread.sleep(1000);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
