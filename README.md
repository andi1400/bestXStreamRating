# bestXStreamRating

bestXStreamRating implements a simple sentiment based ranker of tweets that adhere to a given list of terms in a real-time streaming application using Apache Spark and Apache Flink. Two separate implementations have been creating implementing the same streaming workflow with both Spark and Flink. This project has been build in order to explore and compare the stream processing capabilities of these systems. Additionally a web application is provided to allow for visual comparison of the two systems.

The terms that are analyzed can be anything - it has been tested analyzing for politicians of upcoming elections or names of well-known companies.

## Architecture

The project is organized as a Maven multi-module project which is made up by four modules.

``bestXStreamRatingUtils`` contains the common parts used in both the Spark and Flink implementation. Most of the program logic for the transformations and the sentiment analysis can be found here.

``bestXStreamRatingSpark`` contains the project for running as streaming job on Apache Spark.

``bestXStreamRatingFlink`` contains the project for running as streaming job on Apache Flin.

``bestXStreamRatingWebApp`` contains the web application that is organized as an npm conform Node.JS application.

**The architecture of the streaming applications is described in detail in [this document.](../master/projectArchitecture.pdf)**

## Prerequisites

The three components of bestXStreamRating can run decoupled and do not necessarily need to be run together. For any of the components you need to have a 

* redis server
* an account for the Twitter Streaming API with an app registered for this project. See [here](https://themepacific.com/how-to-generate-api-key-consumer-token-access-key-for-twitter-oauth/994/) on how to create that.

For building the Spark and Flink components you need

* Scala 2.10
* JDK 7
* Maven

and for running an installation of the respecitve processing framework.

* bestXStreamRatingSpark was developed with Apache Spark 1.4.1
* bestXStreamRatingFlink was developed with Apache Flink 0.10.2

For bestXStreamRatingSpark newer version will most likely work as well while for Flink the API was still in progress of changing at the time of developing this app. 

Additionally, you'll need 

* to provide a configuration file for the terms you want to rank - see the Configuration section below.
* the AFINN-111 sentiment dictionary, which can be obtained from [here](http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010). Simply download the zip file, and unpack it. The file needed is ``AFINN/AFINN-111.txt`` inside the zip archive.

For running the Node.js application you need to install Node.JS and npm first, then go into the bestXStreamRatingWebApp directory and issue an ``npm install`` to install the Node.JS dependencies.

## Build

For building the Spark and Flink app artifacts just run ``mvn clean install`` in the root of the repository and everything should build automatically. Both bestXStreamRatingSpark and bestXStreamRatingFlink will be build to their own artifacts in their respective target folder. Running the above command directly from bestXStreamRatingSpark or bestXStreamRatingFlink will fail because both of these depend on the common artifact bestXStreamRatingUtils which needs to be build beforehand.

## Deployment 

For deployment make sure you gathered together all prerequisites as described in the respecitve section and your Redis server is up and running.

Both Spark and Flink accept or require the following command line parameters upon execution

|Parameter|Description|Comment|Optional|
| ------------- |:-------------|:-----|:-----:|
| --winlen 40| The length of the window in seconds |  |Y|
| --slidefreq 8|  The number of seconds after which the window slides |  |Y|
| --batchlen 2 |  The length of a batch in Spark streaming| Spark only!  |Y|
| --snlp |  Use StanfordNLP for sentiment analysis  | highly recommended! |Y|
| --dict AFINN-111.txt| The fully qualified path to the sentiment dictionary. |  |N|
| --consumer-key YOUR_CREDENTIALS| The consumer key for connecting to the Twitter API  |  |N|
| --consumer-secret YOUR_CREDENTIALS| The consumer secret for connecting to the Twitter API |  |N|
| --access-token YOUR_CREDENTIALS| The access token for connecting to the Twitter API |  |N|
| --accessToken-secret YOUR_CREDENTIALS|  The access token secret for connecting to the Twitter API |  |N|
| --terms-file terms.csv | The configuration file containing the terms  |  |N|
| --redis-server localhost| The hostname or IP address of the Redis server | This server must be reachable from all worker nodes and the driver.  |Y|
| --redis-port 6379 | The port where the Redis server runs  |  |Y|
| --no-redis | If results should be displayed on the console instead of being pushed to redis  |  |Y|
| --fake-source | If a fake streaming source should be used instead of Twitter | Flink only!|Y|

### bestXStreamRatingFlink

#####Local Execution

In order to run bestXStreamRatingFlink in local mode make sure you start Flink in local streaming mode using start-local-streaming.sh, then run from the bestXStreamRatingFlink directory

```
flink run -c ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating.AnalyzeTwitterBestXSentimentRatingFlink target/bestXStreamRatingFlink-1.0-SNAPSHOT.jar --dict /path/to/your/AFINN-111.txt --access-token YOUR_CREDENTIALS --access-token-secret YOUR_CREDENTIALS --consumer-key YOUR_CREDENTIALS --consumer-secret YOUR_CREDENTIALS ---terms-file /path/to/your/terms.csv --snlp
```

#####YARN CLUSTER

For execution on a Hadoop Yarn cluster upload the AFINN-111.txt file and the terms configuration file to HDFS, then assuming you are in the bestXStreamRatingFlink run for e.g. starting up with 4 yarn worker nodes

```
flink run -m yarn-cluster -yn 4 -c ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating.AnalyzeTwitterBestXSentimentRatingFlink target/bestXStreamRatingFlink-1.0-SNAPSHOT.jar --access-token YOUR_CREDENTIALS --access-token-secret YOUR_CREDENTIALS --consumer-key YOUR_CREDENTIALS --consumer-secret YOUR_CREDENTIALS --terms-file hdfs:///path/to/terms.csv --dict hdfs:///path/to/AFINN-111.txt  --redis-server YOUR_REDIS_IP --snlp
```

### bestXStreamRatingSpark

#####Local Execution

In order to run bestXStreamRatingSpark in local mode make sure run from the bestXStreamRatingSpark directory

```
spark-submit --class ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating.AnalyzeTwitterBestXSentimentRatingSpark target/bestXStreamRatingSpark-1.0-SNAPSHOT.jar --access-token YOUR_CREDENTIALS --access-token-secret YOUR_CREDENTIALS --consumer-key YOUR_CREDENTIALS --consumer-secret YOUR_CREDENTIALS --terms-file /path/to/your/terms.csv --dict /path/to/your/AFINN-111.txt --redis-server localhost --snlp
```

#####YARN CLUSTER

For execution on a Hadoop Yarn cluster upload the AFINN-111.txt file and the terms configuration file to HDFS, then assuming you are in the bestXStreamRatingSpark run for e.g. starting up in your YARN cluster

```
spark-submit --class ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating.AnalyzeTwitterBestXSentimentRatingSpark --master yarn --deploy-mode cluster target/bestXStreamRatingSpark-1.0-SNAPSHOT.jar ---access-token YOUR_CREDENTIALS --access-token-secret YOUR_CREDENTIALS --consumer-key YOUR_CREDENTIALS --consumer-secret YOUR_CREDENTIALS ---terms-file hdfs:///path/to/terms.csv --dict hdfs:///path/to/AFINN-111.txt  --redis-server YOUR_REDIS_IP  --snlp
```

### bestXStreamRatingWebApp

Simply run ``nodejs app.js <REDIS_PORT>`` from the directory bestXStreamRatingWebApp and the web app will be available at http://YOUR_SERVER_NAME_IP:3030/html/page.html

The web application currently assumes that it is running on the same server as the Redis server is running.

## Configuration

### Specifying terms of interest

In order to define what bestXStreamRating is supposed to rank tweets on you need to provide a configuration file in the following format

```
arbitraryidentifier<TAB>Display Name for Webapp<TAB>synonymTermA,synonym term b,synonymC<TAB>http://your_server/image.jpg
```

Hence a file that would rank tweets about fruit might look as follows

```
arbitraryidentifier<TAB>Display Name for Webapp<TAB>synonymTermA,synonym term b,synonymC<TAB>http://your_server/image.jpg
```

An example analyzing tweets about different instances of fruits might look as follows

```
banana	Banana	banana,nanner	https://upload.wikimedia.org/wikipedia/commons/b/b6/3_Bananas.jpg
maracuya	Maracuya	passion fruit,maracuya	https://upload.wikimedia.org/wikipedia/commons/0/0e/Passionfruit_and_cross_section.jpg
apple	Apple	apple	https://upload.wikimedia.org/wikipedia/commons/1/15/Red_Apple.jpg
strawberry	Strawberry	strawberry	https://upload.wikimedia.org/wikipedia/commons/7/7e/Strawberry_BNC.jpg
pineapple	Pineapple	pineapple	https://upload.wikimedia.org/wikipedia/commons/c/cb/Pineapple_and_cross_section.jpg
```

### Securing the Web App

The web applicaion has a HTTP basic authentication in place. The user and password can be set in the file ``bestXStreamRatingWebApp/users.htpasswd`` and default to ``user / user``.

## Development Setup

This section describes how to set up the project from scratch on a plain Ubuntu Linux machine, tested with Ubuntu 14.04.

```
#install prerequisites
sudo apt-get install -y openjdk-7-jdk openjdk-7-jre maven git nano screen npm nodejs redis-server

#download hadoop, spark, flink 
mkdir -p uni/sw
cd uni/sw
wget http://archive.cloudera.com/cdh5/cdh/5/hadoop-2.6.0-cdh5.5.1.tar.gz
wget http://mirror.cogentco.com/pub/apache/spark/spark-1.4.1/spark-1.4.1-bin-hadoop2.4.tgz
wget http://archive.apache.org/dist/flink/flink-0.10.2/flink-0.10.2-bin-hadoop26.tgz

#download scala and install
wget http://www.scala-lang.org/files/archive/scala-2.10.4.deb
sudo dpkg -i scala-2.10.4.deb

#unpack everything
tar xvf flink-0.10.2-bin-hadoop26.tgz
tar xvf hadoop-2.6.0-cdh5.5.1.tar.gz 
tar xvf spark-1.4.1-bin-hadoop2.4.tgz
ln -s flink-0.10.2 flink
ln -s spark-1.4.1-bin-hadoop2.4 spark
ln -s hadoop-2.6.0-cdh5.5.1 hadoop
```

Additionally, you'll need to update your ``PATH`` variable to contain the ``bin`` directory of Hadoop, Spark and Flink. Furthermore, the ``JAVA_HOME`` variable should be set.

Next, clone the repository, install the node dependencies and run the maven build
```
git clone git@github.com:andi1400/bestXStreamRating.git
cd bestXStreamRating/bestXStreamRatingWebApp
npm install
cd ..
mvn clean install
```
Make sure to download the AFINN-111.txt as described in the Prerequisites section, create a configuration file of terms and you should be good to go for deployment in local mode now.

## License

All parts of this project are - unless declared otherwise in the header of any file - released under the terms of the GPL v3 license that can be found in the ``LICENSE`` file in the root of this repository. 
