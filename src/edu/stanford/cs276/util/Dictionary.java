package edu.stanford.cs276.util;


import edu.stanford.cs276.CandidateGenerator;
import edu.stanford.cs276.Config;
import edu.stanford.cs276.LanguageModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dictionary implements Serializable{
  // TODO: This can be replaced with Trie
//  private int termCount;
  private Trie map;


  public Dictionary() {
//    termCount = 0;
    map = new Trie();
  }
  public Dictionary(Trie map) {
//    termCount = 0;
    this.map = map;
  }
  public void add(String term,int count) {
    map.insert(term,count);
  }


  public Map<String,Pair<Double,Integer>> generateKoffCandidates(String query, int distance,
      LanguageModel lm, CandidateGenerator candidateGenerator){
   Map<String,Pair<Double,Integer>> candidateSet = new HashMap<>();
    candidateSet.put(query,new Pair<>(Config.logNoOpProb,0));
    String[] terms = query.split(" ");
    allocateEditDistancesAmongTerms(terms, distance,candidateSet, lm, candidateGenerator);
    return candidateSet;
  }
  private boolean addWrongTerms(String[] terms, PriorityQueue<Pair<Integer,Double>> wrongWords,int distance, LanguageModel lm){
    int count = 0;
    for (int i=0;i<terms.length;++i){
      double score = lm.unigramProbForTerm(terms[i]);
      if (score == 0.0){
        count++;
        if (count <distance){
          wrongWords.add(new Pair<>(i,score));
        }
      }
    }
    if (count > distance){
      // a query full of junk. We give up and let it go
      wrongWords.clear();
      return false;
    }else{
      return true;
    }
  }

  private void allocateEditDistancesAmongTerms(String[] terms, int distance,
      Map<String, Pair<Double, Integer>> candidateSet, LanguageModel lm,
      CandidateGenerator candidateGenerator){
    // from wrong words to score
//    Set<Pair<Integer,Double>> wrongWords = new HashSet<>();
    PriorityQueue<Pair<Integer,Double>> wrongWords = new PriorityQueue<Pair<Integer,Double>>(distance, new IntegerDoublePairDecendingComparator());
    if (!addWrongTerms(terms,wrongWords,distance,lm)){
      return;
    }
    if (wrongWords.size()<distance){
      for(int i=0;i<terms.length-1;++i){
        if (wrongWords.contains(i)||wrongWords.contains(i+1)){
          continue;
        }


        // we use bigram to decide which word is wrong. and we just need to log of count and total is constant and can be ignored
        double scoreBigram = lm.getBigramProbFor(terms[i],terms[i+1]);
        if (wrongWords.size()<distance){
          wrongWords.add(new Pair<>(i,scoreBigram));
        }else{
          if (scoreBigram<wrongWords.peek().getSecond()){
            wrongWords.poll();
            // unigram is tie breaker for two terms
            wrongWords.add(new Pair<>(i+1,scoreBigram));
//            wrongWords.add(new Pair<>(i,scoreBigram));
          }
        }
      }
    }
    //wrong words are term level and candidate sets are query level
    populateCandset(terms,wrongWords,candidateSet,lm,candidateGenerator);
  }

  private void populateCandset(String[] terms, PriorityQueue<Pair<Integer, Double>> wrongWords,
      Map<String, Pair<Double,Integer>> candidateSet, LanguageModel lm,
      CandidateGenerator candidateGenerator) {
    List<List<Pair<String,Integer>>> listOfCandLists = new ArrayList<>();
    Set<Integer> markedWords = new HashSet<>();
    for (Pair<Integer,Double> cand : wrongWords){
      markedWords.add(cand.getFirst());
    }
    for (int i=0;i<terms.length;++i){
      // from candidate word to edit distance
      List<Pair<String,Integer>> canSetPerTerm = new ArrayList<>();
      if (markedWords.contains(i)){
          PriorityQueue<Pair<String,Double>> topSelector = new PriorityQueue<>(Config.candidateSetSize,new StringDoublePairAscendingComparator());
          Map<String,Integer> termToEdit = new HashMap<>();
          map.dfsGen(terms[i].toCharArray(), new HashSet<Character>(Arrays.asList(CandidateGenerator.alphabet))
              ,0,Config.correctionDistance,new StringBuilder(),topSelector,termToEdit,map.root,lm, i==0?null:terms[i-1]);
          for (Pair<String,Double> pair: topSelector){
            canSetPerTerm.add(new Pair<>(pair.getFirst(),termToEdit.get(pair.getFirst())));
          }
      }else{
        canSetPerTerm.add(new Pair<>(terms[i],0));
      }

      listOfCandLists.add(canSetPerTerm);
    }
    StringBuilder sb = new StringBuilder();

//    for (List<Pair<String,Integer>> eachList: listOfCandLists){
//      for ()
//    }
//  }
    PriorityQueue<Pair<String,Double>> pq = new PriorityQueue<>(Config.candidateSetSize, new StringDoublePairAscendingComparator());
    Map<String, Integer> mapToEditDistance = new HashMap<>();
    dfsWithCanset( listOfCandLists, sb, 0,  0, pq,mapToEditDistance, candidateGenerator,lm);
    for (Pair<String,Double> oneCan: pq){
      candidateSet.put(oneCan.getFirst(),new Pair<>(oneCan.getSecond(),mapToEditDistance.get(oneCan.getFirst())));
    }
  }

  private void dfsWithCanset(List<List<Pair<String,Integer>>> eachTermSet,StringBuilder sb,
      int setIndex, int cumEditDiff,PriorityQueue<Pair<String,Double>> pq
      ,  Map<String, Integer> mapToEditDistance, CandidateGenerator generator, LanguageModel lm) {

    if (setIndex == eachTermSet.size()) {
      String newCan = sb.toString();
      if (!mapToEditDistance.containsKey(newCan)){
        if (pq.size()<Config.candidateSetSize ){
          pq.add(new Pair<>(newCan, generator.jointProbScore(newCan,lm)));

        }else{
          double newCandScore = generator.jointProbScore(newCan,lm);
          if (pq.peek().getSecond()<newCandScore ){
            pq.poll();
            pq.add(new Pair<>(sb.toString(),newCandScore));
          }
        }
        mapToEditDistance.put(newCan,cumEditDiff);
      }else if (mapToEditDistance.get(newCan)>cumEditDiff){
        mapToEditDistance.put(newCan,cumEditDiff);
      }


    } else {

      List<Pair<String, Integer>> list = eachTermSet.get(setIndex);
      for (Pair<String, Integer> term : list) {
        int restoreLen = sb.length();
        if (sb.length() > 0) {
          sb.append(" ");
        }
        sb.append(term.getFirst());
        dfsWithCanset(eachTermSet, sb, setIndex + 1, cumEditDiff + term.getSecond(), pq,mapToEditDistance,generator,lm);
        sb.setLength(restoreLen);
      }
    }
  }
//  public void dfsWithTruncation(String orignal, Pair<String,Integer> query, Pair<String, Double> thePair,NoisyChannelModel ncm, LanguageModel lm){
//    String canQuery = query.getFirst();
//    int editDiff = query.getSecond();
//    double runningMax = thePair.getSecond();
//    // to avoid the exception with an eps
//    double noiseChannel = Math.log(ncm.getEditCostModel().editProbability(orignal,canQuery,editDiff)+Config.eps);
//    if (runningMax>noiseChannel){
//      return;
//    }
//    double bayesEstimateLog = noiseChannel;
//    String[] terms = canQuery.split(" ");
//    biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);
//  }
//  private void biGramJointProbForTerms(String[] terms, int i, Trie map, Pair<String, Double> thePair, Pair<String,Integer> query, double bayesEstimateLog, LanguageModel lm){
//    if (bayesEstimateLog <= thePair.getSecond()){
//      return;
//    }
//    if (i==0){
//      bayesEstimateLog += Math.log(lm.unigramProbForTerm(terms[i])+Config.eps);
//      biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);
//    }else if (i==terms.length-2){
//      bayesEstimateLog += Math.log(lm.getBigramProbFor(terms,i,lm)+Config.eps);
//      if (bayesEstimateLog> thePair.getSecond()){
//        thePair.setFirst(query.getFirst());
//        thePair.setSecond(bayesEstimateLog);
//      }
//    }else if (i>terms.length-1){
//      return;
//    }else{
//      bayesEstimateLog += Math.log(lm.getBigramProbFor(terms,i,lm)+Config.eps);
//      biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);
//
//    }
//
//  }

}
