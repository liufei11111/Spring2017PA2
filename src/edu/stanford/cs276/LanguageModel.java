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
//            System.out.println("Line: "+line);
//            System.out.println("Tokens: "+Arrays.toString(tokens));
//            kGramTrieDict.addKGram(tokens,i,i+Config.kOfGrams);
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
//          kGramTrieDict.add(tokens[i]);

        }

      }
//      for (Entry<String,Integer> entry: map.entrySet()){
//        String[] strs = new String[1];
//        strs[0]=entry.getKey();
//        kGramTrieDict.addKGram(strs,0,1,entry.getValue());
//      }
//      map.clear();
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
//    try {
//      if (lm_ == null) {
//        RandomAccessFile f = new RandomAccessFile(Config.languageModelFile, "r");
//        byte[] b = new byte[(int)f.length()];
//        f.readFully(b);
//        lm_ = LanguageModel.deserialize(b);
//        f.close();
//      }
//    } catch (Exception e) {
//      throw e;
////      throw new Exception("Unable to load language model.  You may not have run buildmodels.sh!");
//    }
//    return lm_;
    try {
      if (lm_ == null) {
        FileInputStream fiA = new FileInputStream(Config.languageModelFile);
        ObjectInputStream oisA = new ObjectInputStream(fiA);
        lm_ = (LanguageModel) oisA.readObject();
        for (Entry<String,Integer> entry : lm_.map.entrySet()){
          String[] strs = new String[1];
          strs[0]=entry.getKey();
          lm_.kGramTrieDict.addKGram(strs,0,1,entry.getValue());

        }
//        lm_.map.clear();
//        lm_.kGramStorageState = lm_.serialize();
//        lm_.kGramStorageState = null;
//        lm_.kGramTrieDict = LanguageModel.deserialize(lm_.kGramStorageState );
      }
    } catch (Exception e) {
      throw e;
//      throw new Exception("Unable to load language model.  You may not have run buildmodels.sh!");
    }
    return lm_;
  }

//  private  static Dictionary deserialize(byte[] b) {
//    Dictionary dic = Dictionary.deserialize(b);
////    LanguageModel lmNew = new LanguageModel(dic);
////    System.out.println(lmNew.kGramTrieDict);
//    return dic;
//  }
//  private  byte[] serialize() {
//    return kGramTrieDict.serialize();
//  }
  /**
   * Saves the object (and all associated data) to disk
   */
  public void save() throws Exception {
//    RandomAccessFile f = new RandomAccessFile(Config.languageModelFile, "rw");
//    f.write(this.serialize());
//    f.close();
    FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
    ObjectOutputStream save = new ObjectOutputStream(saveFile);
//    this.kGramStorageState = this.serialize();
//    this.kGramTrieDict = null;
    save.writeObject(this);
    save.close();
  }
}
