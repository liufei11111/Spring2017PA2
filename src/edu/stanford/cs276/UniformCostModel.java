package edu.stanford.cs276;

/**
 * Implement {@link EditCostModel} interface by assuming assuming
 * that any single edit in the Damerau-Levenshtein distance is equally likely,
 * i.e., having the same probability
 */
public class UniformCostModel implements EditCostModel {
	
	private static final long serialVersionUID = 1L;
	
  @Override
  public double editProbability(String original, String R, int distance) {
    // TODO: improve the score here
    return Math.pow(0.25,distance);
  }
}
