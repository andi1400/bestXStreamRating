package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating

/**
 * Created by ajauch on 08.03.16.
 */
class AnalysisResultModel(singleResults: List[SingleAnalysisResultModel]) {
  def toJSON(): Option[String] ={
    var retString = "["

    if(singleResults.nonEmpty){
      //serialize the elemnts
      for(x <- singleResults){
        retString ++= x.toJSON + ","
      }

      //remove the last comma
      retString = retString.dropRight(1)
    }else{
      return None
    }

    retString ++= "]"

    return Option(retString)
  }
}
