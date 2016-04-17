# bestXStreamRating

bestXStreamRating implements a simple sentiment based ranker of tweets that adhere to a give a list of terms in a real-time streaming application using Apache Spark and Apache Flink. It has been build in order to explore and compare the stream processing capabilities of both systems.

The terms that are analyzed can be anything - it has been tested analyzing for politicians of upcoming elections or names of well-known companies.

## Architecture

TO BE DESCRIBED
* architecture of solution
* maven project architecture

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

For running the Node.js application you need to install Node.JS and npm first, then go into the bestXStreamRatingWebApp directory and issue an ``npm install`` to install the Node.JS dependencies.

## Build

For building the Spark and Flink app artifacts just run ``mvn clean install`` in the root of the repository and everything should build automatically. Both bestXStreamRatingSpark and bestXStreamRatingFlink will be build to their own artifacts in their respective target folder. 

## Deployment 

### bestXStreamRatingFlink

TO BE DONE

### bestXStreamRatingSpark

TO BE DONE

### bestXStreamRatingWebApp

Simply run ``nodejs app.js <REDIS_PORT>`` from the directory bestXStreamRatingWebApp and the web app will be available at http://YOUR_SERVER_NAME_IP:3030/html/page.html

## Configuration

## Specifying terms of interest

### Securing the Web App

The web applicaion has a HTTP basic authentication in place. The user and password can be set in the file ``bestXStreamRatingWebApp/users.htpasswd`` and default to ``user / user``.

## Development Setup

This section describes how to set up the project from scratch on a plain Ubuntu Linux machine, tested with Ubuntu 14.04.

TO BE DONE

## License

All parts of this project are - unless declared otherwise in the header of any file - released under the terms of the GPL v3 license that can be found in the ``LICENSE`` file in the root of this repository. 
