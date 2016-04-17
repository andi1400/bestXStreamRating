package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

import java.lang.Iterable

import edu.stanford.nlp.util.StringUtils
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.functions.windowing.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow

import com.jcraft.jsch.{JSch, Identity};

import scala.util.Random
import java.util.concurrent.TimeUnit

import com.redis.RedisClient
import org.apache.flink.api.common.functions.{MapFunction, RichFlatMapFunction, FlatMapFunction, RichMapFunction}
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.json.JSONParseFlatMap
import org.apache.flink.streaming.connectors.twitter.{TwitterFilterSource}
import org.apache.flink.util.Collector
import org.apache.log4j.{Level, Logger}
import scala.collection.JavaConverters._
import org.apache.flink.contrib.streaming.DataStreamUtils

import scala.io.Source

/**
 * Created by ajauch on 05.03.16.
 */
object AnalyzeTwitterBestXSentimentRatingFlink extends SentimentAnalyzeHelperTrait {
  val log = Logger.getLogger(getClass().getName())

  def main(argv: Array[String]) {
    //set command line args
    val args = new Conf(argv);
    log.info("Batch Length in Seconds: " + args.batchlen())
    log.info("Window Length in Seconds: " + args.winlen())
    log.info("Stream filter terms: " + args.termsFile())
    log.info("Using StanfordNLP: " + args.snlp())

    //Stop info logging from spark
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)

    //Redis Server Config
    val redisServer = args.redisServer()
    val useRedis = !args.noRedis()
    val useSSHTunnel = args.sshTunnel()

    val sshPKey = args.sshPrivateKey()

    val sshUser = args.sshUser()
    val sshPort = args.sshPort()

    //open flink execution environment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val normalEnv = ExecutionEnvironment.getExecutionEnvironment

    //read the filter terms
    val filtersTxtFile = normalEnv.readTextFile(args.termsFile())

    //parse the config file that gives us the filter terms - do in parallel, a little overkill maybe
    val filterTermsConfigDS = filtersTxtFile.map((x) => {
      val splitByTab = x.split("\\t");
      val synonyms = splitByTab(2).split(",")

      val singleCFG = new SingleTermConfigurationScala(splitByTab(0), splitByTab(1), synonyms.toList, splitByTab(3))

      //return as tuple so we can have it as map
      (singleCFG.termsID, singleCFG)
    })

    //collect it so that we can build the required config file object
    val filterTermsConfig = new TermConfigurationFileScala(filterTermsConfigDS.collect().toMap)

    //get the array of term that is used by the twitter source
    val filterTerms = filterTermsConfig.getTermFiltersInTwitterFormat()


    //stream contains post text only
    val tweetStream = if(args.fakeSource()){
      env.addSource(new FakeTwitterSource(filterTerms))
    }else{
      env.addSource(new TwitterSource(filterTerms, args.accessToken(), args.accessTokenSecret(), args.consumerKey(), args.consumerSecret(), args.redisServer(), args.redisPort(), "flink-debug"))
    }

    //read the sentiment dict the flink way, mainly to explore flinks capabilities, could also be done in usual scala
    val sentimentDict = normalEnv.readTextFile(args.dict()).map(new MapFunction[String, Tuple2[String, Int]] {
      def map(in: String): Tuple2[String, Int] = {
        val split = in.split("\t")
        return (split(0), split(1).toInt)
      }
    }).collect().groupBy(x => x._1).mapValues(x => x(0)._2).map(identity)

    //replace strange chars and filter empties
    val sentimentsReplacedAndFiltered = tweetStream.map(_.getText()).map(_.replaceAll("(^[^a-z]+|[^a-z]+$)", "")).filter(_.length != 0)

    val useStanfordNLP = args.snlp()

    //assign the sentiments to each filter term
    val sentimentsByFilterWord = sentimentsReplacedAndFiltered.flatMap(new FlatMapFunction[String, Tuple2[String, Tuple2[Int, Int]]]() {
      def flatMap(post: String, outputCollector: Collector[Tuple2[String, Tuple2[Int, Int]]]): Unit = {
        //do the sentiment analysis using the common code
        val sentimentCountsPerFilterWord = assignAndAnalyzePostCommon(post, sentimentDict, filterTermsConfig, useStanfordNLP)

        //println("AnalysisResult: " + sentimentCountsPerFilterWord.toArray)

        //send them all to the output collecter
        sentimentCountsPerFilterWord.foreach(x => outputCollector.collect(x))
      }
    }).keyBy(0) //key by sentiment word to have a keyed data stream for windowing

    //make it a windowed stream
    val sentimentsRankingByTermWindowed = sentimentsByFilterWord.timeWindow(Time.of(args.winlen(), TimeUnit.SECONDS), Time.of(args.slidefreq(), TimeUnit.SECONDS))

    //sum up the sentiment contributions by all the tweets
    val sentValsReduced = sentimentsRankingByTermWindowed.reduce((x, y) => reduceSentimentCountsAndMetaData(x, y))

    //Do postprocessing such as the normalization
    val sentValsPostprocessed = sentValsReduced.map(x => sentimentNormalizer(x))

    //execute the writing to the db.
   if (useRedis) {
      //Use a map function to write out the tuples to redis
      val result = sentValsPostprocessed.map(new RichMapFunction[(String, (Double, Int)), Int]() {
        var redisClient: RedisClient = null

        //on spawning this mapper init the redis connection
        override def open(configuration: Configuration): Unit = {
          redisClient = new RedisClient(redisServer, 6379)
        }
        override def close(): Unit = {
          redisClient.disconnect
        }
        override def map(in: (String, (Double, Int))): Int = {
          val correspondingTermConf = filterTermsConfig.getSingleTermConfigurationByKey(in._1)
          val singleResult = new SingleAnalysisResultModel(in._1, correspondingTermConf.displayName, in._2._1, in._2._2, None, correspondingTermConf.imageURL)

          //push result out to redis in the flink queue.
          //In contrast to the spark version we are pushing here every element
          val resultAsJson = "[" + singleResult.toJSON() + "]"
          redisClient.rpush("flink", resultAsJson)

          //counter for processed elements
          return 1
        }
      })
    }
    //Alternatively write it to standard out
    else {
      //Use a map function to write out the tuples to redis
      val result = sentValsPostprocessed.map(new RichMapFunction[(String, (Double, Int)), String]() {
        override def open(configuration: Configuration): Unit = {
          /* do nothing */
        }
        override def close(): Unit = {
          /* do nothing */
        }
        override def map(in: (String, (Double, Int))): String = {
          val correspondingTermConf = filterTermsConfig.getSingleTermConfigurationByKey(in._1)
          val singleResult = new SingleAnalysisResultModel(in._1, correspondingTermConf.displayName, in._2._1, in._2._2, None, correspondingTermConf.imageURL)
          val resultAsJson = "[" + singleResult.toJSON() + "]"

          //counter for processed elements
          resultAsJson
        }
      }).print()  //<-- should collect the data to the client and print it there.

     //This method did not work on the cluster.
     //val sentValsPostprocessedCollected = DataStreamUtils.collect(sentValsPostprocessed.getJavaStream).asScala
    }

    //invoke the stream
    env.execute("bestXStreamRatingFlink")
  }


}
