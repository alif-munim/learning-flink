package flinklearn.realtime.datasource;

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

public class FileStreamDataGenerator {

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

            //Define a random number generator
            Random random = new Random();

            for(int i=0; i < 10; i++) {

                // Pick random values
                String user = appUser.get(random.nextInt(appUser.size()));

                System.out.println(user);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
