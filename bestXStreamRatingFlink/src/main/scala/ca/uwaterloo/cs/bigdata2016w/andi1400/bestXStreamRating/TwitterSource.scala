package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

import com.redis.RedisClient
import jline.ConsoleOperations
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import twitter4j.conf.ConfigurationBuilder
import twitter4j._

/**
 * Flink Source for the twitter api using twitter 4j.
 */
class TwitterSource(filters: Array[String], token: String, secret: String, consumerKey: String, consumerSecret: String, logHost: String, logPort: Int, debugQueue: String) extends SourceFunction[Status]{
  @transient
  var twitterlock = new Object()

  //instantiate the twitter config
  //add the credentials
  val conf = new ConfigurationBuilder()
  .setOAuthAccessToken(token)
  .setOAuthAccessTokenSecret(secret)
  .setOAuthConsumerKey(consumerKey)
  .setOAuthConsumerSecret(consumerSecret)
  .build()

  var run: Boolean = true

  override def cancel(): Unit = {
    println("twitter4j cancel.")
    run = false
  }

  override def run(sourceContext: SourceContext[Status]): Unit = {
    //init twitter stream
    val stream = new TwitterStreamFactory(conf).getInstance
    stream.addListener(new TwitterListener(sourceContext,logHost, logPort, debugQueue))

    //filtered query
    val query = new FilterQuery(0, null, filters)
    query.language("en")

    stream.filter(query)

    //init lock in case this instance got transferred.
    if(twitterlock == null){
      twitterlock = new Object()
    }

    //need to keep run method away from returning
    while(run){
      Thread.sleep(100)
    }
  }
}

class TwitterListener(sourceContext: SourceContext[Status], logHost: String = "localhost", logPort: Int = 6379, debugQueue: String = "debug-flink") extends StatusListener{
  @transient
  val redis: RedisClient = new RedisClient(logHost, logPort)

  def writeToRedisDebugLog(message: String): Unit = {
    redis.rpush("debug-flink", message)
  }

  override def onStatus(status: Status): Unit = {
    //writeToRedisDebugLog("Flink - Twitter messag received.")
    //for now we extract just the text
    sourceContext.collect(status)
  }

  override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = {
    writeToRedisDebugLog("Exception: StatusDeletionNotice. ")
  }
  override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = {
    writeToRedisDebugLog("Exception: TrackLimitationNotice. NumberOfLimitedStatusses " + numberOfLimitedStatuses)
  }
  override def onStallWarning(warning: StallWarning) = {
    writeToRedisDebugLog("Stall Warining follows...")
    println("Stall Warining:  " + warning.getPercentFull)
    writeToRedisDebugLog("Stall Warining:  " + warning.getPercentFull)
    println(warning.getMessage)
  }
  def onScrubGeo(l: Long, l1: Long) = {}
  override def onException(e: Exception) = {
    writeToRedisDebugLog("onException: " + e.getMessage())
  }
}