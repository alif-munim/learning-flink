package com.flinklearn.realtime.githubkafkaelastic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class ReadProps {

    public static void main(String[] args) {
        ReadProps rp = new ReadProps();
        rp.load();
    }

    public static HashMap<String, String> load() {

        HashMap<String, String> elasticMap = new HashMap<>();

        try (InputStream input = new FileInputStream("config.properties")) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // add properties to a hashmap
            elasticMap.put("elastic.host", prop.getProperty("elastic.host"));
            elasticMap.put("elastic.port", prop.getProperty("elastic.port"));
            elasticMap.put("elastic.user", prop.getProperty("elastic.user"));
            elasticMap.put("elastic.password", prop.getProperty("elastic.password"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // get the property value and print it out
        System.out.println(elasticMap.get("elastic.host"));
        System.out.println(elasticMap.get("elastic.port"));
        System.out.println(elasticMap.get("elastic.user"));
        System.out.println(elasticMap.get("elastic.password"));
        return elasticMap;

    }

}
