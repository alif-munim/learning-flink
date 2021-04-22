# learning-flink
Apache Flink is one of the fastest growing real-time stream processing 
applications used today, used by companies like 
Amazon and Lyft. 

This repository is a playground for me to learn more about flink and stream 
processing through the datastream API, windowing, time processing, mapping, 
and time management operations, as well as Flink's built-in connectors for 
other platforms like Apache Kafka and Elasticsearch.

### Directory Structure
📦src <br/>
┣ 📂main <br/>
ㅤ ┣ 📂java <br/>
ㅤ ㅤ ┣ 📂com.flinklearn.realtime <br/>
ㅤ ㅤ ㅤ ┣ 📂[common]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜MapCountPrinter.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜Utils.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[datasource]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜FileStreamDataGenerator.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜KafkaStreamDataGenerator.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[datastreamapi]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜AuditTrail.java <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜BasicStreamingOperations.java <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜KeyedStreamOperations.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜StreamSplitAndCombine.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[githubkafkaelastic]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜GitHubDataGenerator.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜GitHubElasticSink.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[ipkafkaelastic]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜IPDataGenerator.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜IPElasticSink.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[project]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜BrowserEvent.java <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜BrowserStreamDataGenerator.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜SummaryDurationPipeline.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[state]() <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜StatefulOperations.java <br/>
ㅤ ㅤ ㅤ ┣ 📂[timeprocessing <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜EventTimeOperations.java <br/>
ㅤ ㅤ ㅤ ┗ 📂[windowing]() <br/>
ㅤ ㅤ ㅤ ㅤ ┣ 📜WindowingOperations.java <br/>
ㅤ ㅤ ㅤ ㅤ ┗ 📜WindowJoins.java <br/>
ㅤ ㅤ ┣ 📂flinklearn <br/>
ㅤ ㅤ ┗ 📂learning <br/>
ㅤ ㅤ ㅤ ┗ 📂realtime <br/>
ㅤ ┗ 📂resources <br/>
┗ 📂test <br/>
ㅤ ┗ 📂java <br/>
