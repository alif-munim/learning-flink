# learning-flink
Apache Flink is one of the fastest growing stream processing 
applications used today, used by companies like 
Amazon and Lyft for high throughput real-time data processing. 

This repository is a playground for me to learn more about flink and stream 
processing through the datastream API, windowing, time processing, mapping, 
and time management operations, as well as Flink's built-in connectors for 
other platforms like Apache Kafka and Elasticsearch.

### directory tree

📂 src/main/java <br/>
┣ 📦com.flinklearn.realtime <br/>
┃ ┣ 📂 [**common**]() <br/>
┃ ┣ 📂 [**datasource**]() <br/>
┃ ┣ 📂 [**datastreamapi**]() <br/>
┃ ┣ 📂 [**githubkafkaelastic**](github-kafka-elastic) <br/>
┃ ┣ 📂 [**ipkafkaelastic**](ip-kafka-elastic) <br/>
┃ ┣ 📂 [**project**]() <br/>
┃ ┣ 📂 [**state**]() <br/>
┃ ┣ 📂 [**timeprocessing**]() <br/>
┃ ┗ 📂 [**windowing**]() <br/>


### ip kafka elastic
```
📂 src/main/java
┣ 📦 com.flinklearn.realtime
┃ ┣ 📂 ipkafkaelastic
┃ ┃ ┣ 📜 IPDataGenerator.java
┃ ┃ ┗ 📜 IPElasticSink.java
```

### github kafka elastic
```
📂 src/main/java
┣ 📦 com.flinklearn.realtime
┃ ┣ 📂 githubkafkaelastic
┃ ┃ ┣ 📜 GitHubDataGenerator.java
┃ ┃ ┗ 📜 GitHubElasticSink.java
```

