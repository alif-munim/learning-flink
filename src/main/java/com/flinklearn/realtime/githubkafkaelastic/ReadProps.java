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

    public static Properties load() {

        Properties prop = new Properties();

        try (InputStream input = new FileInputStream("config.properties")) {

            // load a properties file
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // return the hashmap
        return prop;

    }

}
