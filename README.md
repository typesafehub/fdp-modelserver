# Model serving

This is an umbrella project for all things model serving that is comprised of multiple projects

-**akkaserver** - implementation of model scoring and statistics serving using Akka streams and Akka HTTP


-**flinkserver** - implementation of model scoring and queryable state using Flink. Both 
key-based and partition-based approach are implemented here

-**kafkaclient** - generic client used for testing of all implementations (except serving samples)
Reads data files, split them into records, converts to protobuf implementations and publishes them to Kafka

-**kafkaconfiguration** - simple module containing class with Kafka definitions - server location,
topics, etc. used by all applications

-**kafkastreamserver** - implementation of model scoring and queryable state using Kafka streams
Also includes implementation of custom Kafka streams store.

-**model** - implementation of support classes representing model and model factories used by all applications. 
Because Kafka streams is Java and the rest of implementations are Scala, there are two versions of these 
classes - Java and Scala           

-**serving samples** - This module contains simple implementations of model scoring using PMML and 
tensorflow model definitions. It is not using any streaming frameworks - just straight Scala code

-**protobufs** - a module containing protobufs that are used for all streaming frameworks. 
This protobufs describe model and data definition in the stream. Because Kafka streams is Java 
and the rest of implementations are Scala, both Java and Scala implementations of protobufs are 
generated


-**sparkML** - examples of using SparkML for machine learning and exporting results to PMML
using JPMML evaluator for Spark - https://github.com/jpmml/jpmml-evaluator-spark

-**sparkserver** - implementation of model scoring using Spark

-**utils** - a module containing some utility code. Most importantly it contains embedded Kafka implementation
which can be used for testing in the absence of kafka server. In order to use it, just add these 
lines to your code:
         
         
    // Create embedded Kafka and topics
    EmbeddedSingleNodeKafkaCluster.start()                      // Create and start the cluster 
    EmbeddedSingleNodeKafkaCluster.createTopic(DATA_TOPIC)      // Add topic
    EmbeddedSingleNodeKafkaCluster.createTopic(MODELS_TOPIC)    // Add topic
      
If you are using both server and client add kafka embedded only to server and start it before the client
In addition to embedded kafka this module there are some utility classes used by all applications. 
Because Kafka streams is Java and the rest of implementations are Scala, there are two versions of these 
classes - Java and Scala  

-**data** - a directory of data files used as sources for all applications

Not included in this project are:

-**Beam implementation** - Beam Flink runner is still on Scala 2.10 so it is in its own
separate project - ???

-**Python/Tensorflow/Keras** - is it is a Python so it is in its own
separate project - ???
      