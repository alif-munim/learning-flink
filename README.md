# learning-flink
Apache Flink is one of the fastest growing real-time stream processing 
applications used today, used by companies like 
Amazon and Lyft. 

This repository is a playground for me to learn more about flink and stream 
processing through the datastream API, windowing, time processing, mapping, 
and time management operations, as well as Flink's built-in connectors for 
other platforms like Apache Kafka and Elasticsearch.

### Directory Structure

📦src/main/java <br/>
┣ 📂com.flinklearn.realtime <br/>
┃ ┣ 📂[**common**]() <br/>
┃ ┃ ┣ MapCountPrinter.java <br/>
┃ ┃ ┗ Utils.java <br/>
┃ ┣ 📂[**datasource**]() <br/>
┃ ┃ ┣ FileStreamDataGenerator.java <br/>
┃ ┃ ┗ KafkaStreamDataGenerator.java <br/>
┃ ┣ 📂[**datastreamapi**]() <br/>
┃ ┃ ┣ AuditTrail.java <br/>
┃ ┃ ┣ BasicStreamingOperations.java <br/>
┃ ┃ ┣ KeyedStreamOperations.java <br/>
┃ ┃ ┗ StreamSplitAndCombine.java <br/>
┃ ┣ 📂[**githubkafkaelastic**]() <br/>
┃ ┃ ┣ GitHubDataGenerator.java <br/>
┃ ┃ ┗ GitHubElasticSink.java <br/>
┃ ┣ 📂[**ipkafkaelastic**]() <br/>
┃ ┃ ┣ IPDataGenerator.java <br/>
┃ ┃ ┗ IPElasticSink.java <br/>
┃ ┣ 📂[**project**]() <br/>
┃ ┃ ┣ BrowserEvent.java <br/>
┃ ┃ ┣ BrowserStreamDataGenerator.java <br/>
┃ ┃ ┗ SummaryDurationPipeline.java <br/>
┃ ┣ 📂[**state**]() <br/>
┃ ┃ ┗ StatefulOperations.java <br/>
┃ ┣ 📂[**timeprocessing**]() <br/>
┃ ┃ ┗ EventTimeOperations.java <br/>
┃ ┗ 📂[**windowing**]() <br/>
┃ ┃ ┣ WindowingOperations.java <br/>
┃ ┃ ┗ WindowJoins.java <br/>
