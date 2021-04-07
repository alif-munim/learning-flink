package com.flinklearn.realtime.datasource;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/****************************************************************************
 * Create browser session data generator which continuously produces info
 * about the user, action, and timestamp through Kafka records.
 ****************************************************************************/

public class BrowserStreamDataGenerator implements Runnable {

    /****************************************************************************
     * ANSI Color Codes
     ****************************************************************************/
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) {
        BrowserStreamDataGenerator bsdg = new BrowserStreamDataGenerator();
        bsdg.run();
    }

    public void run() {

        try {

            /****************************************************************************
             * Configure properties and create a flink kafka producer
             ****************************************************************************/
            Properties properties = new Properties();
            properties.put("bootstrap.servers", "localhost:9092");
            properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            // Create a kafka producer with strings for key and value serialization
            KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

            /****************************************************************************
             * Create user, and action data lists to be randomly selected from
             ****************************************************************************/

            List<String> userList = new ArrayList<>();
            userList.add("Grover");
            userList.add("Elmo");
            userList.add("Bert");
            userList.add("Ernie");

            List<String> actionList = new ArrayList<>();
            actionList.add("Login");
            actionList.add("Logout");
            actionList.add("View Product");
            actionList.add("Add to Cart");
            actionList.add("Purchase");

            Random random = new Random();

            /****************************************************************************
             * Loop 100 times, randomly selecting data and marking time with each pass
             ****************************************************************************/

            for(int i = 0; i < 100; i++) {
                String user = userList.get(random.nextInt(userList.size()));
                String action = actionList.get(random.nextInt(actionList.size()));

                String currentTime = String.valueOf(System.currentTimeMillis());

                String[] browserEvent = {
                  String.valueOf(i),
                  user,
                  action,
                  currentTime
                };

                String recordKey = currentTime;
                ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(
                        "flink.summary.duration",
                        recordKey,
                        String.join(",", browserEvent)
                );

                RecordMetadata recordMetadata = producer.send(producerRecord).get();

                System.out.println(ANSI_PURPLE + "Browser: Sending Event : "
                        + String.join(", ", browserEvent)  + ANSI_RESET);

                Thread.sleep(500);

            }

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
