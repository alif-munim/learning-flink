# learning-flink
Apache Flink is one of the fastest growing real-time stream processing 
applications used today, used by companies like 
Amazon and Lyft. 

This repository is a playground for me to learn more about flink and stream 
processing through the datastream API, windowing, time processing, mapping, 
and time management operations, as well as Flink's built-in connectors for 
other platforms like Apache Kafka and Elasticsearch.

### Directory Structure
📦src
┣ 📂main
. ┣ 📂java
. . ┣ 📂com.flinklearn.realtime
. . . ┣ 📂common
. . . . ┣ 📜MapCountPrinter.java
. . . . ┗ 📜Utils.java
. . . ┣ 📂datasource
. . . . ┣ 📜FileStreamDataGenerator.java
. . . . ┗ 📜KafkaStreamDataGenerator.java
. . . ┣ 📂datastreamapi
. . . . ┣ 📜AuditTrail.java
. . . . ┣ 📜BasicStreamingOperations.java
. . . . ┣ 📜KeyedStreamOperations.java
. . . . ┗ 📜StreamSplitAndCombine.java
. . . ┣ 📂githubkafkaelastic
. . . . ┣ 📜GitHubDataGenerator.java
. . . . ┗ 📜GitHubElasticSink.java
. . . ┣ 📂ipkafkaelastic
. . . . ┣ 📜IPDataGenerator.java
. . . . ┗ 📜IPElasticSink.java
. . . ┣ 📂project
. . . . ┣ 📜BrowserEvent.java
. . . . ┣ 📜BrowserStreamDataGenerator.java
. . . . ┗ 📜SummaryDurationPipeline.java
. . . ┣ 📂state
. . . . ┗ 📜StatefulOperations.java
. . . ┣ 📂timeprocessing
. . . . ┗ 📜EventTimeOperations.java
. . . ┗ 📂windowing
. . . . ┣ 📜WindowingOperations.java
. . . . ┗ 📜WindowJoins.java
. . ┣ 📂flinklearn
. . ┗ 📂learning
. . . ┗ 📂realtime
. ┗ 📂resources
┗ 📂test
. ┗ 📂java
