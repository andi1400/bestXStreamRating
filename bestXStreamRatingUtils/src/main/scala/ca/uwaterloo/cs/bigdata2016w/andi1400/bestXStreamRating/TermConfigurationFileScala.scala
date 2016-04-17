package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

import scala.collection.mutable.ListBuffer


/**
 * Created by ajauch on 31.03.16.
 */
class TermConfigurationFileScala(terms_c: scala.collection.Map[String, SingleTermConfigurationScala]) extends Serializable{
  val terms = terms_c

  def getSingleTermConfigurationByKey(key: String): SingleTermConfigurationScala = {
    terms.apply(key)
  }

  def getTermFiltersInTwitterFormat(): Array[String] = {
    val l = ListBuffer.empty[String]

    //just add all terms and all synonyms
    for(x <- terms){
      for(y <- x._2.synonyms){
        l += y
      }
    }

    l.toArray
  }
}
