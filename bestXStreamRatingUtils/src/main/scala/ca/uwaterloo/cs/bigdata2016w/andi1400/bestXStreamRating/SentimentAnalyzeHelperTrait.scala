package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating


/**
 * Created by ajauch on 05.03.16.
 */
trait SentimentAnalyzeHelperTrait {
  /**
   * Analyze a twitter post text for sentiment by one of the provided methods:
   * a) StanfordNLP
   * b) static dictionary
   *
   * @param post The tweet text to analyze.
   * @param wordSentiments A Map[String,Int] that contains the static sentiment dictionary.
   * @param filterConfiguration A List[String] that contains the terms that should be analyzed for.
   * @param useStanfordNLP A boolean indicating if Stanford NLP should be used.
   * @return
   */
  def assignAndAnalyzePostCommon(post: String, wordSentiments: Map[String, Int], filterConfiguration: TermConfigurationFileScala, useStanfordNLP: Boolean):  Iterable[(String, (Int, Int))] = {
    //tokenize the post
    val tokenizedPost = post.split( """\s+""").filter(_.length != 0).toSet

    //expand it to all the matches in the filter
    val matchingFilterWordsFull = filterConfiguration.terms.filter((x) => {
      val matchedSyns = x._2.synonyms.filter(s => tokenizedPost.contains(s.toLowerCase))

      !matchedSyns.isEmpty
      }
    )

    //for continuing we only need the ids since thats the only thing we store
    val matchingFilterWords = matchingFilterWordsFull.map(_._2.termsID)

    //if empty early return
    if (matchingFilterWords.isEmpty) {
      return Iterable[(String, (Int, Int))]()
    }

    //ok matches - do sentiment analysis
    val sentimentCount = if (!useStanfordNLP) {
      analyzeSentiment(post, tokenizedPost, wordSentiments, false)
    } else {
      analyzeSentiment(post, tokenizedPost, null, true)
    }

    //assign the sentiment to all of the matching filter values
    return matchingFilterWords.map(x => (x, (sentimentCount, 1)))
  }

  //reduceByKey: simple reducer that sums up all the numbers
  //in: a: (sentiment, counter=1), b: (sentiment, counter=1)
  //out: (sentimentSum, counterSum)
  def reduceValuesSentimentCountsAndMetaData(a:(Int, Int), b:(Int, Int)): (Int, Int) = {
    return (a._1 + b._1, a._2 + b._2)
  }

  def reduceSentimentCountsAndMetaData(a:(String, (Int, Int)), b:(String, (Int, Int))): (String, (Int, Int)) = {
    if(!a._1.equals(b._1)){
      throw new RuntimeException("Reducing tuples with different words! There has to be an error.")
    }

    return (a._1, reduceValuesSentimentCountsAndMetaData(a._2, b._2))
  }

  /**
   * Analyze the sentiment of the post using either StanfordNLP or a primitive sentiment dictionary.
   *
   * The result is the sentiment for this post in the range of -2 and 2. With 0 being neutral.
   *
   * @param post
   * @param tokenizedPost
   * @param useStanfordNLP
   * @return
   */
  def analyzeSentiment(post: String, tokenizedPost: Iterable[String], wordSentiments: Map[String, Int], useStanfordNLP: Boolean): Int = {
    //assign the sentiment counts to each individual word
    val sentimentCount: Int = if(useStanfordNLP){
      SentimentUtils.analyzeSentiment(post)
    }else {
      val sentimentCountPerTerm = tokenizedPost.map(w => wordSentiments.getOrElse(w, 0))

      //sum all the sentiment counts up to get the sentiment for the whole tweet
      sentimentCountPerTerm.reduce(_ + _)
    }

    return sentimentCount
  }

  //map: normalize sentiment by the counter, go through a sigmoid function to have a clearer distinguishing sentiment
  //in (term, sentimentSum, counterSum)
  //out (term, sentimentSum / counterSum, counterSum)
  def sentimentNormalizer(tuple: (String, (Int, Int))): (String, (Double, Int)) = {
    val sigmoidSentiment =  1.0 / (1.0 + Math.exp(-2* tuple._2._1.toDouble / tuple._2._2))

    return (tuple._1, (sigmoidSentiment, tuple._2._2))
  }

  //generate the result as a json structure for writing to output
  def generateResultAsJSONString(entity: Iterator[(String, (Double, Int))], termConfigurationFile: TermConfigurationFileScala): Option[String] = {
    //push all of the results into a list in order to have them in the right format
    val buf = scala.collection.mutable.ListBuffer.empty[SingleAnalysisResultModel]
    for (e <- entity) {
      val termConf = termConfigurationFile.getSingleTermConfigurationByKey(e._1)

      buf += new SingleAnalysisResultModel(e._1, termConf.displayName, e._2._1, e._2._2, None, termConf.imageURL);
    }

    //serialize it
    return new AnalysisResultModel(buf.toList).toJSON
  }
}
