package com.flinklearn.realtime.project;

/****************************************************************************
 * Create flink pipeline which does the following:
 * (1) Compute 10-second summaries containing user, action, and event count
 * (2) Compute the duration for each action and print user, action, duration
 ****************************************************************************/

public class SummaryDuration {

    public static void main(String[] args) {

        try {

            /****************************************************************************
             * Create stream execution environment
             ****************************************************************************/

            /****************************************************************************
             * Configure and create flink kafka consumer
             ****************************************************************************/

            /****************************************************************************
             * Add consumer as a streaming source and convert to data stream
             ****************************************************************************/

            /****************************************************************************
             * Output 10-second summaries using event time operations
             ****************************************************************************/

            /****************************************************************************
             * Perform stateful operations to print event durations
             ****************************************************************************/

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
