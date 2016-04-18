# bestXStreamRating

bestXStreamRating implements a simple sentiment based ranker of tweets that adhere to a given list of terms in a real-time streaming application using Apache Spark and Apache Flink. Two separate implementations have been creating implementing the same streaming workflow with both Spark and Flink. This project has been build in order to explore and compare the stream processing capabilities of these systems. Additionally a web application is provided to allow for visual comparison of the two systems.

The terms that are analyzed can be anything - it has been tested analyzing for politicians of upcoming elections or names of well-known companies.

## Architecture

The project is organized as a Maven multi-module project which is made up by four modules.

``bestXStreamRatingUtils`` contains the common parts used in both the Spark and Flink implementation. Most of the program logic for the transformations and the sentiment analysis can be found here.

``bestXStreamRatingSpark`` contains the project for running as streaming job on Apache Spark.

``bestXStreamRatingFlink`` contains the project for running as streaming job on Apache Flin.

``bestXStreamRatingWebApp`` contains the web application that is organized as an npm conform Node.JS application.

The architecture of the streaming applications is described in detail in [this document.](../blob/master/LICENSE)

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

### bestXStreamRatingFlink

TO BE DONE

### bestXStreamRatingSpark

TO BE DONE

### bestXStreamRatingWebApp

Simply run ``nodejs app.js <REDIS_PORT>`` from the directory bestXStreamRatingWebApp and the web app will be available at http://YOUR_SERVER_NAME_IP:3030/html/page.html

## Configuration

### Specifying terms of interest

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
Make sure to download the AFINN-111.txt as described in the Prerequisites section, create a configuration file of terms and you should be good to go.

## License

All parts of this project are - unless declared otherwise in the header of any file - released under the terms of the GPL v3 license that can be found in the ``LICENSE`` file in the root of this repository. 
