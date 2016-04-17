package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

/**
 * Created by ajauch on 31.03.16.
 */
class SingleTermConfigurationScala(termID_c: String, displayName_c: String, synonyms_c: List[String], imageURL_c: String) extends Serializable{
  val termsID = termID_c
  val displayName = displayName_c
  val synonyms = synonyms_c
  val imageURL = imageURL_c
}
