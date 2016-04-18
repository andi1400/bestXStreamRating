package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

import org.rogach.scallop._

class Conf(args: Seq[String]) extends ScallopConf(args){
  mainOptions = Seq(winlen)
  val winlen = opt[Int](descr = "length of window (s)", required = false, default = Some(40))
  val slidefreq = opt[Int](descr = "length of window (s)", required = false, default = Some(8))
  val batchlen = opt[Int](descr = "length of window (s)", required = false, default = Some(2))
  val snlp = opt[Boolean](descr = "use StanfordNLP for sentiment analysis", required = false, default = Some(false))
  val dict = opt[String](descr = "sentiment dictionary path", required = false, default = Some("AFINN-111.txt"))

  //the terms used for filtering
  val termsFile = opt[String](descr = "terms to filter twitter stream", required = false, default = Some("terms.csv"))

  //twitter stuff
  val consumerKey= opt[String](descr = "Twitter consumer key", required = true)
  val consumerSecret= opt[String](descr = "Twitter consumer secret", required = true)
  val accessToken = opt[String](descr = "Twitter access token", required = true)
  val accessTokenSecret = opt[String](descr = "Twitter access token secret", required = true)

  //redis server
  val redisServer = opt[String](descr = "redis server ip or hostname", required = false, default = Some("localhost"))
  val redisPort = opt[Int](descr = "redis port", required = false, default = Some(6379))
  val noRedis = opt[Boolean](descr = "do not use redis as target output", required = false, default = Some(false))

  val fakeSource = opt[Boolean](descr = "do not use real twitter stream", required = false, default = Some(false))

}