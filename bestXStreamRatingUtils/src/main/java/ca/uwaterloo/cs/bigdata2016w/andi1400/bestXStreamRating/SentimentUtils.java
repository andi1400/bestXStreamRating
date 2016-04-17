package ca.uwaterloo.cs.bigdata2016w.andi1400.bestXStreamRating;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

import java.util.Properties;

/**
 * Created by ajauch on 16.02.16.
 */
public class SentimentUtils {
    private static Properties props;
    private static StanfordCoreNLP pipeline;

    //static initializer block to set up stuff for sentiment analysis
    static {
        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(props);
    }

    /**
     * Method to perform sentiment analysis using the StanfordNLP Package. It will
     * try to find the sentiment for this string by assuming the sentiment is determined
     * by the biggest sentence of the String.
     *
     * TODO possible improvement ==> using the avg sentiment!! Or use median sentiment
     *
     *
     *
     * @param s The string to analyze
     * @return the sentiment between -2 and 2 or null in case of an error.
     */
    public static Integer analyzeSentiment(String s){
        Integer foundSentiment = null;
        if (s != null && s.length() > 0) {
            int longest = 0;
            Annotation annotation = pipeline.process(s);
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                Tree tree = sentence.get(SentimentCoreAnnotations.AnnotatedTree.class);
                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);

                String partText = sentence.toString();

                //we always store the sentiment for the longest sentence in the tweet - although a tweet
                //presumably should not be many sentences
                if (partText.length() > longest) {
                    foundSentiment = sentiment;
                    longest = partText.length();
                }

            }
        }
        
        //center the found sentiment at 0 so we can use it in combination with other sentiments in addition
        if(foundSentiment != null){
            foundSentiment -= 2;
        }

        //finally do some error checking
        if(foundSentiment < -2 || foundSentiment > 2){
            return null;
        }

        return foundSentiment;
    }
}
