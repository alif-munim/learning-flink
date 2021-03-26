package com.flinklearn.realtime.datasource;

import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/*
* Import info, research:
* [] com.opencsv.CSVWriter
* [] org.apache.commons.io.FileUtils
* [] java.io.File vs. java.io.FileWriter
* */



public class FileStreamDataGenerator implements Runnable {

    // ANSI escape codes for color
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_BLUE = "\u001B[34m";

    public static void main(String[] args) {
        FileStreamDataGenerator fsdg = new FileStreamDataGenerator();
        fsdg.run();
    }

    public void run() {
        try {

            // Define a list of users
            List<String> appUser = new ArrayList();
            appUser.add("Harry");
            appUser.add("Ron");
            appUser.add("Hermione");

            // Define a list of operations
            List<String> appOperation = new ArrayList();
            appOperation.add("Create");
            appOperation.add("Modify");
            appOperation.add("Query");
            appOperation.add("Delete");

            // Define a list of app entities
            List<String> appEntity = new ArrayList();
            appEntity.add("Customer");
            appEntity.add("SalesRep");

            //Define a random number generator
            Random random = new Random();

            // Define data output directory
            String dataDir = "data/raw_audit_trail";

            //Clean out existing files in the directory
            FileUtils.cleanDirectory(new File(dataDir));

            // Create 100 random records
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

                // Open a new file for this record
                FileWriter auditFileWriter = new FileWriter(
                        dataDir + "/audit_trail_" + i + ".csv"
                );

                // Write csv to file
                CSVWriter auditCSVWriter = new CSVWriter(auditFileWriter);
                auditCSVWriter.writeNext(csv);

                // Log output
                System.out.println(
                        ANSI_BLUE + "FileStream Generator : Creating File : " +
                        Arrays.toString(csv) + ANSI_RESET
                );

                // Close and flush writer
                auditCSVWriter.flush();
                auditCSVWriter.close();

                // Sleep before next record
                Thread.sleep(1000);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
