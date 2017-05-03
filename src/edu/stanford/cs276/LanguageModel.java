package edu.stanford.cs276;

import edu.stanford.cs276.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;

import edu.stanford.cs276.util.Dictionary;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * LanguageModel class constructs a language model from the training corpus.
 * This model will be used to score generated query candidates.
 * 
 * This class uses the Singleton design pattern
 * (https://en.wikipedia.org/wiki/Singleton_pattern).
 */
public class LanguageModel implements Serializable {
	
	private static final long serialVersionUID = 1L;
  private static LanguageModel lm_;
  HashMap<String,Integer> map = new HashMap<>();
  Long uniqWordCount = 0L;
  public HashMap<Pair<String,String>,Integer> bigram = new HashMap<Pair<String, String>, Integer>();
  Dictionary kGramTrieDict = null;
//  byte[] kGramStorageState = null;

  /*
   * Feel free to add more members here (e.g., a data structure that stores bigrams)
   */

  /**
   * Constructor
   * IMPORTANT NOTE: you should NOT change the access level for this constructor to 'public', 
   * and you should NOT call this constructor outside of this class.  This class is intended
   * to follow the "Singleton" design pattern, which ensures that there is only ONE object of
   * this type in existence at any time.  In most circumstances, you should get a handle to a 
   * NoisyChannelModel object by using the static 'create' and 'load' methods below, which you
   * should not need to modify unless you are making substantial changes to the architecture
   * of the starter code.  
   *
   * For more info about the Singleton pattern, see https://en.wikipedia.org/wiki/Singleton_pattern.  
   */
  private LanguageModel(String corpusFilePath) throws Exception {
    kGramTrieDict = new Dictionary();
    constructDictionaries(corpusFilePath);

  }


  /**
   * This method is called by the constructor, and computes language model parameters 
   * (i.e. counts of unigrams, bigrams, etc.), which are then stored in the class members
   * declared above.  
   */
  public void constructDictionaries(String corpusFilePath) throws Exception {

    System.out.println("Constructing dictionaries...");
    File dir = new File(corpusFilePath);
    for (File file : dir.listFiles()) {
      if (".".equals(file.getName()) || "..".equals(file.getName())) {
        continue; // Ignore the self and parent aliases.
      }
      System.out.printf("Reading data file %s ...\n", file.getName());
      BufferedReader input = new BufferedReader(new FileReader(file));
      String line = null;
      while ((line = input.readLine()) != null) {
        /*
         * Remember: each line is a document (refer to PA2 handout)
         *
         */
        String[] tokens = line.split(" ");
        for (int i=0;i<tokens.length;++i){
          if (i!=tokens.length-1){
            Pair<String,String> bigramPair = new Pair<>(tokens[i],tokens[i+1]);
            if (bigram.containsKey(bigramPair)){
              bigram.put(bigramPair,bigram.get(bigramPair)+1);
            }else{
              bigram.put(bigramPair,1);
            }
          }
          if (map.containsKey(tokens[i])){
            map.put(tokens[i],map.get(tokens[i])+1);

          }else{
            map.put(tokens[i],1);
          }
          uniqWordCount++;
        }

      }
      input.close();
    }
    System.out.println("Done.");
  }

  /**
   * Creates a new LanguageModel object from a corpus. This method should be used to create a
   * new object rather than calling the constructor directly from outside this class
   */
  public static LanguageModel create(String corpusFilePath) throws Exception {
    if (lm_ == null) {
      lm_ = new LanguageModel(corpusFilePath);
    }
    return lm_;
  }

  /**
   * Loads the language model object (and all associated data) from disk
   */
  public static LanguageModel load() throws Exception {
    try {
      if (lm_ == null) {
        FileInputStream fiA = new FileInputStream(Config.languageModelFile);
        ObjectInputStream oisA = new ObjectInputStream(fiA);
        lm_ = (LanguageModel) oisA.readObject();
        for (Entry<String,Integer> entry : lm_.map.entrySet()){
          lm_.kGramTrieDict.add(entry.getKey(),entry.getValue());

        }
      }
    } catch (Exception e) {
      throw e;
//      throw new Exception("Unable to load language model.  You may not have run buildmodels.sh!");
    }
    return lm_;
  }

  /**
   * Saves the object (and all associated data) to disk
   */
  public void save() throws Exception {
//    RandomAccessFile f = new RandomAccessFile(Config.languageModelFile, "rw");
//    f.write(this.serialize());
//    f.close();
    FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
    ObjectOutputStream save = new ObjectOutputStream(saveFile);
    save.writeObject(this);
    save.close();
  }
  private double rawCountForTerm(String term){
    Integer count = map.get(term);
    return count == null? Config.eps:count;
  }
  private double rawBiCountForTerms(String term1, String term2){
    Integer count = bigram.get(new Pair<>(term1,term2));
    return count == null? Config.eps:count;
  }
  public double unigramProbForTerm(String term) {
    Integer count = map.get(term);
    if (count == null){
      return 0.0;
    }else{
      return Math.log(count)-Math.log(uniqWordCount);
    }
  }
  public double getConditionalProd(String term1, String term2) {
    double unigramScore = unigramProbForTerm(term2);
    if (term1 == null){
      return unigramScore*2;
    }
    double termUnigram = rawCountForTerm(term1);
    double countBigram = rawBiCountForTerms(term1,term2);
    double bigramScore = Math.log(countBigram)-Math.log(termUnigram+Config.eps);
    // we use bigram to decide which word is wrong. and we just need to log of count and total is constant and can be ignored
    return  unigramScore*Config.smoothingFactor+bigramScore*(1-Config.smoothingFactor);
  }


}
