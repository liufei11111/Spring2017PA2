package edu.stanford.cs276;

public class Config {
  public static final String noisyChannelFile = "noisyChannel";
  public static final String languageModelFile = "languageModel";
  public static final int distance = 2; // number of edit's allowed in query
  public static final int correctionDistance = 2; // for a term, edit distance
  public static final double smoothingFactor = 0.01;
  public static final double languageModelScalingFactor = 0.1;
  public static final double eps = 1e-25;
  public static final int candidateSetSize = 10;
//  public static final double charEditProb = 1-1e32;// given that it is selected
  public static final Double logNoOpProb = -125.0;
}
