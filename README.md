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
A basic example of Apache Flink's built-in Kafka and Elasticsearch connectors in use. Adapted for
Elasticsearch 7 from [Kiera Zhou's implementation](https://github.com/keiraqz/KafkaFlinkElastic).
```
📂 src/main/java
┣ 📦 com.flinklearn.realtime
┃ ┣ 📂 ipkafkaelastic
┃ ┃ ┣ 📜 IPDataGenerator.java
┃ ┃ ┗ 📜 IPElasticSink.java
```
[`IPDataGenerator.java`](): Randomly generates 100 comma-delimited string arrays containing an _ip
address_ and _connection name_. These strings are produced to the **ip.info** kafka topic.<br><br>
[`IPElasticSink.java`](): Consumes string array data from the **ip.info** kafka topic, extracts the 
data into a HashMap, and bulk posts it to the **ip-test** elastic index.


### github kafka elastic
A flink pipeline that consumes data from kafka, performs some transformations, 
and routes the data to the appropriate index in a secure elasticsearch cluster.
```
📂 src/main/java
┣ 📦 com.flinklearn.realtime
┃ ┣ 📂 githubkafkaelastic
┃ ┃ ┣ 📜 GitHubDataGenerator.java
┃ ┃ ┗ 📜 GitHubElasticSink.java
```

[`GitHubDataGenerator.java`](): Randomly generates 5000 JSON string records containing
the _id_, _type_, _user_, and _branch_ of a given **pull request** or **issue**. These records are then
produced to either the **pullrequest** or **issue** topic.<br><br>
[`GithubElasticSink.java`](): Consumes messages from the **pullrequest** and **issue** topics and merges the
two data streams. A secure elasticsearch sink is configured based on properties specified in a 
`config.properties` file, and added to the merged data stream. The data routing method is specified in 
an elasticsearch sink function. A `.setBulkFlushMaxActions()` property is also set so that the messages
are posted in bulk to elastic, for greater efficiency.
