package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

import java.util.Date

import scala.util.Random
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import twitter4j._
/**
 * Flink Source for faking the twitter connection for easier debugging
 */
class FakeTwitterSource(filters: Array[String]) extends SourceFunction[Status]{
  var run: Boolean = true

  override def cancel(): Unit = {
    println("fake twitter cancel.")
    run = false
  }

  override def run(sourceContext: SourceContext[Status]): Unit = {
    while(run){
      //throw dice to which terms this twee shall be assigned
      val s = for (i <- 1 to 2) yield (filters(Random.nextInt(filters.size)))

      //throw another dice for deciding if positive or negative
      val goodness = if(Random.nextInt(2) <= 1){
        "bad"
      }else{
        "good"
      }
      val res = s.mkString(" ") +  " "  + goodness

      Thread.sleep(Random.nextInt(500))
      sourceContext.collect(new FakeTweet(res))
    }
  }
}

//FakeTwee object to satisfy twitter4j api
class FakeTweet(text: String) extends Status{
    override def getQuotedStatus: Status = null

    override def getPlace: Place = null

    override def isRetweet: Boolean = false

    override def isFavorited: Boolean = false

    override def getFavoriteCount: Int = 0

    override def getCreatedAt: Date = new Date()

    override def getWithheldInCountries: Array[String] = null

    override def getUser: User = null

    override def getContributors: Array[Long] = null

    override def getRetweetedStatus: Status = null

    override def getInReplyToScreenName: String = null

    override def getLang: String = "en"

    override def isTruncated: Boolean = false

    override def getId: Long = 0

    override def isRetweeted: Boolean = false

    override def getCurrentUserRetweetId: Long = 0

    override def isPossiblySensitive: Boolean = false

    override def getRetweetCount: Int = 1

    override def getGeoLocation: GeoLocation = null

    override def getInReplyToUserId: Long = 0

    override def getSource: String = null

    override def getText: String = {
      return text
    }

    override def getInReplyToStatusId: Long = 0

    override def getScopes: Scopes = null

    override def isRetweetedByMe: Boolean = false

    override def getQuotedStatusId: Long = 0

    override def compareTo(t: Status): Int = -1

    override def getAccessLevel: Int = 0

    override def getRateLimitStatus: RateLimitStatus = null

    override def getHashtagEntities: Array[HashtagEntity] = null

    override def getURLEntities: Array[URLEntity] = null

    override def getSymbolEntities: Array[SymbolEntity] = null

    override def getMediaEntities: Array[MediaEntity] = null

    override def getUserMentionEntities: Array[UserMentionEntity] = null

    override def getExtendedMediaEntities: Array[ExtendedMediaEntity] = null
}

