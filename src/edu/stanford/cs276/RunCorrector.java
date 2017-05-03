package edu.stanford.cs276;

import edu.stanford.cs276.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RunCorrector {

  public static LanguageModel languageModel;
  public static NoisyChannelModel nsm;
  public static CandidateGenerator canGen;
  public static void main(String[] args) throws Exception {
    
    // Parse input arguments
    String uniformOrEmpirical = null;
    String queryFilePath = null;
    String goldFilePath = null;
    String extra = null;
    BufferedReader goldFileReader = null;
    
    if (args.length == 2) {
      // Default: run without extra credit code or gold data comparison
      uniformOrEmpirical = args[0];
      queryFilePath = args[1];
    } 
    else if (args.length == 3) {
      uniformOrEmpirical = args[0];
      queryFilePath = args[1];
      if (args[2].equals("extra")) {
        extra = args[2];
      } else {
        goldFilePath = args[2];
      }
    } 
    else if (args.length == 4) {
      uniformOrEmpirical = args[0];
      queryFilePath = args[1];
      extra = args[2];
      goldFilePath = args[3];
    } 
    else {
      System.err.println(
          "Invalid arguments.  Argument count must be 2, 3 or 4 \n"
          + "./runcorrector <uniform | empirical> <query file> \n"
          + "./runcorrector <uniform | empirical> <query file> <gold file> \n"
          + "./runcorrector <uniform | empirical> <query file> <extra> \n"
          + "./runcorrector <uniform | empirical> <query file> <extra> <gold file> \n"
          + "SAMPLE: ./runcorrector empirical data/queries.txt \n"
          + "SAMPLE: ./runcorrector empirical data/queries.txt data/gold.txt \n"
          + "SAMPLE: ./runcorrector empirical data/queries.txt extra \n"
          + "SAMPLE: ./runcorrector empirical data/queries.txt extra data/gold.txt \n");
      return;
    }

    if (goldFilePath != null) {
      goldFileReader = new BufferedReader(new FileReader(new File(goldFilePath)));
    }

    // Load models from disk
    languageModel = LanguageModel.load();
    nsm = NoisyChannelModel.load();
    canGen = CandidateGenerator.get();
    BufferedReader queriesFileReader = new BufferedReader(new FileReader(new File(queryFilePath)));
    nsm.setProbabilityType(uniformOrEmpirical);
    String query = null;

    /*
     * Each line in the file represents one query. We loop over each query and find
     * the most likely correction
     */
    FileWriter fw = new FileWriter(new File("./pa2-diff.txt"));
    FileWriter fw2 = new FileWriter(new File("./pa2-word-diff.txt"));
    FileWriter fw3 = new FileWriter(new File("./pa2-space-diff.txt"));
    while ((query = queriesFileReader.readLine()) != null) {
      Map<String,Pair<Double,Integer>> queries = canGen.getCandidates(query,languageModel.kGramTrieDict,languageModel);
      queries= canGen.filterStopWords(queries);
      Pair<String,double[]> correctedQuery =  canGen.getCorrectedQuery(query,queries,nsm,languageModel);
      queries.clear();// force GC
      /*
       * Your code here: currently the correctQuery and original query are the same
       * Complete this implementation so that the spell corrector corrects the 
       * (possibly) misspelled query
       * 
       */
      
      if ("extra".equals(extra)) {
        /*
         * If you are going to implement something regarding to running the corrector,
         * you can add code here. Feel free to move this code block to wherever
         * you think is appropriate. But make sure if you add "extra" parameter,
         * it will run code for your extra credit and it will run you basic
         * implementations without the "extra" parameter.
         */
      }

      // If a gold file was provided, compare our correction to the gold correction
      // and output the running accuracy
      if (goldFileReader != null) {
        String goldQuery = goldFileReader.readLine();
        /*
         * You can do any bookkeeping you wish here - track accuracy, track where your solution
         * diverges from the gold file, what type of errors are more common etc. This might
         * help you improve your candidate generation/scoring steps 
         */
        if (!goldQuery.equals(correctedQuery.getFirst())){
          double[] scores = correctedQuery.getSecond();
          fw.write("original: "+query+", GoldQuery: "+goldQuery+", MyQuery: "+correctedQuery.getFirst()+" Scores: "+ Arrays.toString(scores)+"\n");
          String[] goldterms = goldQuery.split(" ");
          String[] myterms = correctedQuery.getFirst().split(" ");
          if (goldterms.length != myterms.length) {
            fw3.write(
                "original: " + query + ", GoldQuery: " + goldQuery + ", MyQuery: " + correctedQuery
                    .getFirst() + "\n");
          }else{
            List<Pair<String,String>> listDiff = new LinkedList<>();
            for (int i=0;i<goldterms.length;++i){
              if (!goldterms[i].equals(myterms[i])){
                listDiff.add(new Pair<>(goldterms[i],myterms[i]));
              }
            }
            StringBuilder sb = new StringBuilder();
            for (Pair<String,String> pair:listDiff){
              sb.append(pair+" ");
            }
            fw2.write("original: "+query+", GoldQuery: "+goldQuery+", MyQuery: "+correctedQuery.getFirst()+"________"+sb.toString()+"\n");
          }
        }
      }
      
      /*
       * Output the corrected query.
       * IMPORTANT: In your final submission DO NOT add any additional print statements as 
       * this will interfere with the autograder
       */
      System.out.println(correctedQuery.getFirst());
    }
    fw.close();
    queriesFileReader.close();
  }
}
