package edu.stanford.cs276;

public class Config {
  public static final String noisyChannelFile = "noisyChannel";
  public static final String languageModelFile = "languageModel";
  public static final int distance = 1; // number of edit's allowed in query
  public static final int correctionDistance = 2; // for a term, edit distance
  public static final double smoothingFactor = 0.1;
  public static final double languageModelScalingFactor = 1.0;
  public static final double eps = 1e-25;
  public static final int candidateSetSize = 3;
  public static final double charEditProb = 0.99;// given that it is selected
  public static final Double logNoOpProb = Math.log(1-charEditProb);
}
