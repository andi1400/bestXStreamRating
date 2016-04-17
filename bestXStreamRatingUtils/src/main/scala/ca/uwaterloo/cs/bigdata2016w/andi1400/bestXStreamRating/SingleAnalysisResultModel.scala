package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

/**
 * Created by ajauch on 08.03.16.
 */
class SingleAnalysisResultModel(term: String, displayName: String, sentiment: Double, occurence: Int, numRetweets: Option[Int], imageURL: String) {
  def toJSON(): String = {
    var retString = "{"

    //MANDATORY FIELDS

    //term - the id
    retString ++= "\"name\":"
    retString ++= "\""
    retString ++= term
    retString ++= "\","

    //displayName
    retString ++= "\"displayName\":"
    retString ++= "\""
    retString ++= displayName
    retString ++= "\","

    //sentiment
    retString ++= "\"sentiment\":"
    retString ++= sentiment.toString
    retString ++= ","

    //occurence
    retString ++= "\"occurence\":"
    retString ++= occurence.toString

    //term image
    retString ++= ","
    retString ++= "\"image\":"
    retString ++= "\""
    retString ++= imageURL.toString
    retString ++= "\""

    //NON MANDATORY FIELDS

    //numRetweets
    if(numRetweets.isDefined){
      retString ++= ","
      retString ++= "\"retweets\":"
      retString ++= numRetweets.get.toString
    }

    retString ++= "}"

    return retString
  }
}