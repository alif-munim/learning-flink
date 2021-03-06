# learning-flink
Apache Flink is one of the fastest growing stream processing 
applications used today, used by companies like 
Amazon and Lyft for high throughput real-time data processing. 

This repository is a playground for me to learn more about flink and stream 
processing through the datastream API, windowing, time processing, mapping, 
and time management operations, as well as Flink's built-in connectors for 
other platforms like Apache Kafka and Elasticsearch.
<br><br>

### directory tree
ð src/main/java <br/>
â£ ð¦com.flinklearn.realtime <br/>
â â£ ð common <br/>
â â£ ð datasource <br/>
â â£ ð datastreamapi <br/>
â â£ ð [**githubkafkaelastic**](#github-kafka-elastic) <br/>
â â£ ð [**ipkafkaelastic**](#ip-kafka-elastic) <br/>
â â£ ð project <br/>
â â£ ð state <br/>
â â£ ð timeprocessing <br/>
â â ð windowing <br/>
<br><br>

## prerequisites
The instructions below are written for Windows. However, the steps should be relatively similar on Mac OS as well. 
The following prerequisites are also the same.
* Kafka installed. [Download here](https://kafka.apache.org/downloads.html).
* Elasticsearch 7 installed. [Download here](https://www.elastic.co/downloads/elasticsearch).
* [Recommended] IntelliJ Idea installed. [Download here](https://www.jetbrains.com/idea/download/).
* [Recommended] Postman installed. [Download here](https://www.postman.com/downloads/).
<br><br>
  
## quickstart
**project setup:** <br>
Clone this github repository, and import it into the Java IDE of your choice. [IntelliJ Idea](https://www.jetbrains.com/idea/download/)
is recommended.
```
> git clone https://github.com/alif-munim/learning-flink.git
```
<br>

**kafka steps**: <br>
start the kafka server and zookeeper, and then create the topics required for this project. 
```
> zookeeper-server-start.bat ../../config/zookeeper.properties
```
```
> kafka-server-start.bat ../../config/server.properties
```
```
> kafka-topics.bat --create --topic [ip.info|pullrequest|issue] --bootstrap-server localhost:9092
```
<br>

You can also
start a console producer to produce messages to the topic of your choice.
```
> kafka-console-producer.bat --topic [ip.info|pullrequest|issue] --bootstrap-server localhost:9092
```
<br>

**elasticsearch steps**: <br>
start a local elasticsearch. If you are connecting to an existing secure 
elasticsearch instance, you can skip this step.
```
> elasticsearch.bat
```
<br>

**config steps**: <br>
create a file called `config.properties` in the project root directory. Specify the
properties below. If your elastic instance is not secure, you can skip _elastic.user_ and _elastic.password_.
```
elastic.host=[host]
elastic.port=[port]
elastic.user=[user]
elastic.password=[password]
kafka.host=[host]
kafka.port=[port]
kafka.security.protocol=[SSL]
kafka.ssl.truststore.location=[truststore.jks]
kafka.ssl.truststore.password=[password]
```
<br><br>

## ip kafka elastic
A basic example of Apache Flink's built-in Kafka and Elasticsearch connectors in use. Adapted for
Elasticsearch 7 from [Kiera Zhou's implementation](https://github.com/keiraqz/KafkaFlinkElastic).
```
ð src/main/java
â£ ð¦ com.flinklearn.realtime
â â£ ð ipkafkaelastic
â â â£ ð IPDataGenerator.java
â â â ð IPElasticSink.java
```
[**IPDataGenerator.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/ipkafkaelastic/IPDataGenerator.java): Randomly generates 100 comma-delimited string arrays containing an _ip
address_ and _connection name_. These strings are produced to the **ip.info** kafka topic.<br>
[**IPElasticSink.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/ipkafkaelastic/IPElasticSink.java): Consumes string array data from the **ip.info** kafka topic, extracts the 
data into a HashMap, and bulk posts it to the **ip-test** elastic index.
<br><br><br>

## github kafka elastic
A flink pipeline that consumes data from a secure kafka, performs some transformations, 
and routes the data to the appropriate index in a secure elasticsearch cluster.
```
ð src/main/java
â£ ð¦ com.flinklearn.realtime
â â£ ð githubkafkaelastic
â â â£ ð GitHubDataGenerator.java
â â â£ ð GitHubDataGenerator.java
â â â£ ð ReadProps.java
â â â ð SecureKafka.java
```
[**GitHubDataGenerator.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/githubkafkaelastic/GitHubDataGenerator.java): Randomly generates 5000 JSON string records containing
the _id_, _type_, _user_, and _branch_ of a given **pull request** or **issue**. These records are then
produced to either the **pullrequest** or **issue** topic.<br>
[**GithubElasticSink.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/githubkafkaelastic/GitHubElasticSink.java): Consumes messages from the **pullrequest** and **issue** topics and merges the
two data streams. A secure elasticsearch sink is configured based on properties specified in a 
`config.properties` file, and added to the merged data stream. The data routing method is specified in 
an elasticsearch sink function. A `.setBulkFlushMaxActions()` property is also set so that the messages
are posted in bulk to elastic, for greater efficiency. <br>
[**ReadProps.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/githubkafkaelastic/ReadProps.java)
Read configuration properties from the `config.properties` file. <br>
[**SecureKafka.java**](https://github.com/alif-munim/learning-flink/blob/master/src/main/java/com/flinklearn/realtime/githubkafkaelastic/SecureKafka.java): Experimenting reading from a secure kafka instance using SSL.
