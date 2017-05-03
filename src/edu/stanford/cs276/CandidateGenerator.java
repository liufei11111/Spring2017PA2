package edu.stanford.cs276;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Pair;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CandidateGenerator implements Serializable {

	private static final long serialVersionUID = 1L;
	private static CandidateGenerator cg_;

  /** 
  * Constructor
  * IMPORTANT NOTE: As in the NoisyChannelModel and LanguageModel classes, 
  * we want this class to use the Singleton design pattern.  Therefore, 
  * under normal circumstances, you should not change this constructor to 
  * 'public', and you should not call it from anywhere outside this class.  
  * You can get a handle to a CandidateGenerator object using the static 
  * 'get' method below.  
  */
  private CandidateGenerator() {}

  public static CandidateGenerator get() throws Exception {
    if (cg_ == null) {
      cg_ = new CandidateGenerator();
    }
    return cg_;
  }

  public static final Character[] alphabet = { 'a', 'b', 'c', 'd', 'e', 'f',
      'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', ' ', ',' };
  public static final Character[] numberAndSpecialCharSet = {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', ' ', ',','-','_','.', '&','!','(',')','[',']','#','$','%','^','+',"'".toCharArray()[0]
  };
  public static final String[] stopWordsSet = {
      "the","a","they","it"
  };
  // Generate all candidates for the target query
  public Map<String, Pair<Double, Integer>> getCandidates(String query,Dictionary dic,LanguageModel lm) throws Exception {
    return dic.generateKoffCandidates(query,Config.distance,lm,this);
  }


  public Pair<String,double[]> getCorrectedQuery(String original, Map<String,Pair<Double,Integer>> queries,NoisyChannelModel ncm, LanguageModel lm) {
    Pair<String, double[]> thePair = null;
    for (Entry<String,Pair<Double, Integer>> query: queries.entrySet()){
      // everything is already log transformed
//      double noisyScore = ncm.getEditCostModel().editProbability(original,query.getKey(),query.getValue().getSecond());
//      double languageScore = query.getValue().getFirst();
//      double candScore = noisyScore+Config.languageModelScalingFactor * languageScore;
//
//      if (thePair == null){
//        thePair = new Pair<>(query.getKey(),candScore);
//      }else if (thePair.getSecond()<candScore){
//        thePair = new Pair<>(query.getKey(),candScore);
//      }
      double noisyScore = ncm.getEditCostModel().editProbability(original,query.getKey(),query.getValue().getSecond());
      double languageScore = query.getValue().getFirst();
      double candScore = noisyScore+Config.languageModelScalingFactor * languageScore;
      double[] scores = new double[3];
      scores[0]=noisyScore;
      scores[1]=languageScore;
      scores[2]=candScore;
      if (thePair == null){
        thePair = new Pair<>(query.getKey(),scores);
      }else if (thePair.getSecond()[2]<candScore){
        thePair = new Pair<>(query.getKey(),scores);
      }
    }
    if (thePair == null){
      throw new RuntimeException("Forbidden query cands without a single result!");
    }
    // TODO: delete this test section!!!!!
    List<Entry<String,Pair<Double,Integer>>> list = new ArrayList<>();
    Collections.sort(list, new Comparator<Entry<String, Pair<Double, Integer>>>() {
      @Override
      public int compare(Entry<String, Pair<Double, Integer>> o1,
          Entry<String, Pair<Double, Integer>> o2) {
        return o1.getValue().getFirst().compareTo(o2.getValue().getFirst());
      }
    });
    for (Entry<String,Pair<Double,Integer>> entry : list){
      System.out.println(entry.getKey()+": "+entry.getValue());
    }
    // TODO: delete this test section!!!!!
    return thePair;
  }

  public  double jointProbScore(String query, LanguageModel lm) {
    String[] terms = query.split(" ");
    double logLanguageScore = 0.0; // log 1
    for(int i=0;i<terms.length-1;++i){
      if (i==0) {
        // raw count is good as the total count is a constant
        logLanguageScore += lm.unigramProbForTerm(terms[i]);
      }

      logLanguageScore+=lm.getConditionalProd(terms[i],terms[i+1]);

    }
    return logLanguageScore;
  }

  public Map<String,Pair<Double,Integer>> filterStopWords(Map<String, Pair<Double, Integer>> queries) {
    for (String str : stopWordsSet){
      if (queries.containsKey(str)&&queries.get(str).getSecond()>0){
        queries.remove(str);
      }
    }return queries;
  }
}
