package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating;

import org.apache.log4j._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.redis._

object AnalyzeTwitterBestXSentimentRatingSpark extends SentimentAnalyzeHelperTrait{
  val log = Logger.getLogger(getClass().getName())

  //flat map function to do the sentiment analysis, on each post
  //and in the case a post
  //out: (term, sentiment, counter=1)
  def assginAndAnalyzePosts(post: String, wordSentiments: org.apache.spark.broadcast.Broadcast[Map[String, Int]], filterWords: org.apache.spark.broadcast.Broadcast[TermConfigurationFileScala], useStanfordNLP: Boolean = true):  Iterable[(String, (Int, Int))] =  {
    val sentimentCountsPerFilterWord: Iterable[(String, (Int, Int))] = assignAndAnalyzePostCommon(post, wordSentiments.value, filterWords.value, useStanfordNLP)

    //println("sentiment count per filter word  " + sentimentCountsPerFilterWord + " for " + post)
    return sentimentCountsPerFilterWord
  }


  //mapPartitions: Method to post final result to redis for the webapp to consume
  def writeResultToRedis(entity: Iterator[(String, (Double, Int))], redisServer: String = "localhost", redisPort: Int = 6379, termConfigurationFile: TermConfigurationFileScala): Iterator[Unit] ={
    val res: Option[String] = generateResultAsJSONString(entity, termConfigurationFile)

    //open redis server connection
    val redis: RedisClient = new RedisClient(redisServer, redisPort)

    //push result to redis
    if (res.isDefined) {
      redis.rpush("spark", res.get)
    }

    //return empty iterator to satisfy Spark API
    return Iterator.empty
  }


  /*
     * Driver Program
     */
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

    //Twitter authentication properties
    System.setProperty("twitter4j.oauth.consumerKey", args.consumerKey())
    System.setProperty("twitter4j.oauth.consumerSecret", args.consumerSecret())
    System.setProperty("twitter4j.oauth.accessToken", args.accessToken())
    System.setProperty("twitter4j.oauth.accessTokenSecret", args.accessTokenSecret())

    val useSNLP = args.snlp()

    //Redis Server Config
    val redisServer = args.redisServer()
    val redisPort = args.redisPort()
    val useRedis =  !args.noRedis()

    //Create Spark App and Configure
    val conf = new SparkConf().setAppName("AnalyzeTwitterBestXSentimentRatingSpark")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, Seconds(args.batchlen()))

    //read config file into a string
    val filterConfigurationAsStr = sc.textFile(args.termsFile())

    //read input config but dont do validation
    val filterConfigurationSingles = filterConfigurationAsStr.map(x => {
      val splitByTab = x.split("\\t");
      val synonyms = splitByTab(2).split(",")

      val singleCFG = new SingleTermConfigurationScala(splitByTab(0), splitByTab(1), synonyms.toList, splitByTab(3))

      //return tuples [String, SingleTermConfiguration] such that we will have a map of this
      (singleCFG.termsID, singleCFG)
    }).collectAsMap()

    //create the surrounding object that we will broadcast
    val filterConfiguration = new TermConfigurationFileScala(filterConfigurationSingles)

    //broadcast configuration
    val filterConfigurationBC = sc.broadcast(filterConfiguration)

    //create twitter stream - use self-adopted version from original spark version of spark 1.3
    val twitterStream = new TwitterFilterableDStream(ssc, None, filterConfiguration.getTermFiltersInTwitterFormat(), "en", StorageLevel.MEMORY_AND_DISK_SER_2)

    //For sentiment analysis we start with simple word sentiment list
    val sentimentDict = args.dict()

    //Get te sentiments map if we don't use stanford nlp. Can't rellay use the option trait here
    //since its a spark broadcast variable. So need to use old null pattern.
    val wordSentiments = ssc.sparkContext.textFile(sentimentDict).map { line =>
      val Array(word, happinessValue) = line.split("\t")
      (word, happinessValue.toInt)
    }

    val wordSentimentsCollected: Map[String, Int] = wordSentiments.collectAsMap().toMap
    val wordSentimentsAsMap: Broadcast[Map[String, Int]] = sc.broadcast(wordSentimentsCollected)

    //post text only
    val postTextOnly = twitterStream.map(post => post.getText)

    //replace strange chars, and clean out empty
    val tweetsCleaned = postTextOnly.map(_.replaceAll("(^[^a-z]+|[^a-z]+$)", "")).filter(_.length != 0)

    //assign the sentiments to each filter term
    val sentimentsByFilterWord = tweetsCleaned.flatMap(x => assginAndAnalyzePosts(x, wordSentimentsAsMap, filterConfigurationBC, useSNLP))

    //now sum over this in the window
    val sentimentsRankingByTermWindowed = sentimentsByFilterWord.reduceByKeyAndWindow((a: (Int, Int),b: (Int, Int)) => reduceValuesSentimentCountsAndMetaData(a,b), Seconds(args.winlen()), Seconds(args.slidefreq()))

    //normalize sentiment
    val sentimentNormalized = sentimentsRankingByTermWindowed.map(sentimentNormalizer)

    // Deal with result - either redis or stdout
    sentimentNormalized.foreachRDD(rdd => {
      //println("Processing reduced window for writing to redis server. Count: " + rdd.count())

      //In Spark do the writing as mini-batch wise in mapPartitions.
      //execute the writing to the db. CollectAsync needed to have an action triggering the operation.
      if(useRedis){
        rdd.mapPartitions((x) => writeResultToRedis(x, redisServer, redisPort, filterConfigurationBC.value)).collectAsync()
      }
      //Alternatively write it to standard out
      else{
        rdd.mapPartitions((partition) => {
          println("Processing partition..")
          for(el <- partition){
            println(el.toString())
          }

          //expected to return an iterator in mapPartitions - so we return an empty iterator to satisfy Spark.
          Iterator.empty
        }).collectAsync()
      }
    })

    //run the stream to the end ==> forever
    ssc.start()
    ssc.awaitTermination()
  }
}