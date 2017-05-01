package edu.stanford.cs276.util;


import edu.stanford.cs276.CandidateGenerator;
import edu.stanford.cs276.Config;
import edu.stanford.cs276.LanguageModel;
import edu.stanford.cs276.NoisyChannelModel;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.RandomAccessFile;

public class Dictionary implements Serializable{
  // TODO: This can be replaced with Trie
//  private int termCount;
  private Trie map;


  public static Dictionary deserialize(byte[] b){
    return new Dictionary(Trie.deserialize(b,0,b.length));
  }
  public byte[] serialize(){
    return map.serialize();
  }
  public Dictionary() {
//    termCount = 0;
    map = new Trie();
  }
  public Dictionary(Trie map) {
//    termCount = 0;
    this.map = map;
  }
  public void add(String term) {
//    termCount++;
    List<String> list = new LinkedList<>();
    list.add(term);
    map.insert(list);
  }
  public void addKGram(String[] terms, int startI, int endI,int count) {
//    termCount++;
    List<String> list = Arrays.asList(terms);
    map.insert(list.subList(startI,endI),0,count);
  }
//  public int count(String term) {
//    return map.searchWordNodePos(term).wordCount;
//  }

  public Set<Pair<String,Integer>> generateKoffCandidates(String query, int distance, LanguageModel lm){
    Set<Pair<String,Integer>> candidateSet = new HashSet<>();
    candidateSet.add(new Pair<String, Integer>(query,0));
    String[] terms = query.split(" ");
    allocateEditDistancesAmongTerms(terms, distance,candidateSet, lm);
    return candidateSet;
  }
  private void addWrongTerms(String[] terms, Set<Pair<Integer,Double>> wrongWords,int distance){
    for (int i=0;i<terms.length&&wrongWords.size()<=distance;++i){
      double score = map.unigramProbForTerm(terms[i]);
      if (score == 0.0){
        wrongWords.add(new Pair<>(i,score));
      }
    }
  }

  private void allocateEditDistancesAmongTerms(String[] terms,  int distance, Set<Pair<String,Integer>> candidateSet,LanguageModel lm){
    // from wrong words to score
    Set<Pair<Integer,Double>> wrongWords = new HashSet<>();
    addWrongTerms(terms,wrongWords,distance);
    int selectedIndex = -1;
    double currentLowProd = 1;
    if (wrongWords.size()<distance){
      for(int i=0;i<terms.length-1&&wrongWords.size()<=Config.distance;++i){
        if (wrongWords.contains(i)||wrongWords.contains(i+1)){
          continue;
        }
        // we use bigram to decide which word is wrong. We don't need K gram for this. Rather, we use
        // K gram for deciding which candidate is the best corrected one.
        double scoreBigram = map.getBigramProbFor(terms,i,lm)*map.unigramProbForTerm(terms[i]);
        if (scoreBigram<currentLowProd){
          selectedIndex = i;
          currentLowProd = scoreBigram;
        }
      }
      if (selectedIndex >= 0){
        wrongWords.add(new Pair<>(selectedIndex,currentLowProd));
        wrongWords.add(new Pair<>(selectedIndex+1,currentLowProd));
      }
    }
    //wrong words are term level and candidate sets are query level
    populateCandset(terms,wrongWords,candidateSet);
  }
  private void trimCanSetWithBigram(Set<String> canSetPerTerm,String[] terms,int i, int j){
    // TODO: imporve Trimming
    Set<String> reservedSet = new HashSet<>();
    if (canSetPerTerm.size()>Config.candidateSetSize){
      int k=0;
      for (String str: canSetPerTerm){
        if (k<Config.candidateSetSize){
          reservedSet.add(str);
          k++;
        }else {
          break;
        }
      }
      canSetPerTerm.retainAll(reservedSet);
    }
  }
  private void populateCandset(String[] terms, Set<Pair<Integer, Double>> wrongWords,
      Set<Pair<String, Integer>> candidateSet) {
    List<List<Pair<String,Integer>>> eachTermSet = new ArrayList<>();
    Map<String, Integer> termToEditDistance = new HashMap<>();
    Set<Integer> markedWords = new HashSet<>();
    for (Pair<Integer,Double> cand : wrongWords){
      markedWords.add(cand.getFirst());
    }
    for (int i=0;i<terms.length;++i){
      Set<String> canSetPerTerm = new HashSet<>();
      if (markedWords.contains(i)){
        for (int j=1;j<=Config.correctionDistance;++j){
          map.dfsGen(terms[i].toCharArray(), new HashSet<Character>(Arrays.asList(CandidateGenerator.alphabet))
              ,0,i,new StringBuilder(),canSetPerTerm,map.root);
          trimCanSetWithBigram(canSetPerTerm,terms,i,j);
          for (String str : canSetPerTerm){
            if (!termToEditDistance.containsKey(str)){
              termToEditDistance.put(str,j);
            }
          }

        }
      }else{
        canSetPerTerm.add(terms[i]);
        if (!termToEditDistance.containsKey(terms[i])){
          termToEditDistance.put(terms[i],0);
        }
      }
      List<Pair<String,Integer>> eachList = new ArrayList<>();
      for (String term : canSetPerTerm){
        eachList.add(new Pair<>(term, termToEditDistance.get(term)));
      }
      eachTermSet.add(eachList);
    }
    StringBuilder sb = new StringBuilder();
    dfsWithCanset( eachTermSet, sb, 0,  0, candidateSet);

  }

  private void dfsWithCanset(List<List<Pair<String,Integer>>> eachTermSet,StringBuilder sb, int setIndex, int cumEditDiff, Set<Pair<String, Integer>> candidateSet){
    if (setIndex==eachTermSet.size()){
      candidateSet.add(new Pair<>(sb.toString(),cumEditDiff));
    }else{

      List<Pair<String,Integer>> list = eachTermSet.get(setIndex);
      for (Pair<String,Integer> term : list){
        int restoreLen = sb.length();
        sb.append(term.getFirst());
        dfsWithCanset(eachTermSet,sb,setIndex+1,cumEditDiff+term.getSecond(),candidateSet);
        sb.setLength(restoreLen);
      }
    }
  }
  public String getCorrectedQuery(String original, Set<Pair<String,Integer>> queries,NoisyChannelModel ncm, LanguageModel lm) {
    Double probLowThreshold = Double.MIN_VALUE;
    Pair<String, Double> thePair = null;
    for (Pair<String, Integer> query: queries){
      if (thePair == null){
        thePair = new Pair<>(query.getFirst(),probLowThreshold);
        continue;
      }
      dfsWithTruncation(original, query, thePair, ncm, lm);
    }
    if (thePair == null){
      throw new RuntimeException("Forbidden query cands without a single result!");
    }
    return thePair.getFirst();
  }
  public void dfsWithTruncation(String orignal, Pair<String,Integer> query, Pair<String, Double> thePair,NoisyChannelModel ncm, LanguageModel lm){
    String canQuery = query.getFirst();
    int editDiff = query.getSecond();
    double runningMax = thePair.getSecond();
    // to avoid the exception with an eps
    double noiseChannel = Math.log(ncm.getEditCostModel().editProbability(orignal,canQuery,editDiff)+Config.eps);
    if (runningMax>noiseChannel){
      return;
    }
    double bayesEstimateLog = noiseChannel;
    String[] terms = canQuery.split(" ");
    biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);
  }
  private void biGramJointProbForTerms(String[] terms, int i, Trie map, Pair<String, Double> thePair, Pair<String,Integer> query, double bayesEstimateLog, LanguageModel lm){
    if (bayesEstimateLog <= thePair.getSecond()){
      return;
    }
    if (i==0){
      bayesEstimateLog += Math.log(map.unigramProbForTerm(terms[i])+Config.eps);
      biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);
    }else if (i==terms.length-2){
      bayesEstimateLog += Math.log(map.getBigramProbFor(terms,i,lm)+Config.eps);
      if (bayesEstimateLog> thePair.getSecond()){
        thePair.setFirst(query.getFirst());
        thePair.setSecond(bayesEstimateLog);
      }
    }else if (i>terms.length-1){
      return;
    }else{
      bayesEstimateLog += Math.log(map.getBigramProbFor(terms,i,lm)+Config.eps);
      biGramJointProbForTerms(terms,0,map,thePair,query,bayesEstimateLog,lm);

    }

  }

//  public String  getCorrectedQuery(String query){
//    for
//    Set<String> candidateSet = new HashSet<>();
//    for (int i=1;i<distance;++i){
//      map.dfsGen(term.toCharArray(), new HashSet<Character>(Arrays.asList(CandidateGenerator.alphabet)),
//          0,i,new StringBuilder(),candidateSet,map.root,0);
//    }
//    candidateSet.add(term);
//    return candidateSet;
//  }
}
