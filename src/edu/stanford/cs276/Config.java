package edu.stanford.cs276;

public class Config {
  public static final String noisyChannelFile = "noisyChannel";
  public static final String languageModelFile = "languageModel";
  public static final String candidateGenFile = "candidateGenerator";
  public static final int distance = 2; // number of edit's allowed in query
  public static final int correctionDistance = 1; // for a term, edit distance
  public static final int kOfGrams = 2;
  public static final double smoothingFactor = 0.1;
  public static final double eps = 1e-250;
  public static final int hashMapInitialSize = 32;
  public static final int candidateSetSize = 5;
}
