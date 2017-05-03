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
    // Assume each character has prob k being editted there are x edits with prob
    // combinorial(x,n)*k^x*k^(n-x) with n be the length and each edits is equal weight
    // take the log will go away. So, we after loging we have log(n)-log(x), log(n-1)-log(x-1),...., log(n-x+1)-log(1) for the combinatorial term
    //  and x*log(k)+(n-x)*log(1-k) . n is constant for all queries.x * (log(k)-log(1-k)) is changing.
    // we have log(n)-log(x), log(n-1)-log(x-1),...., log(n-x+1)-log(1) as rough estimate of edit prob.
//    double n = original.length();
//    double x = distance;
//    double k = Config.charEditProb;
//    double cum = 0;
//
//    cum += x*(Math.log(k)-Math.log(1-k));
//    for (int i=0;i<distance-1;++i){
//      cum += Math.log(n)-Math.log(x);
//      n-=1;
//      x-=1;
//    }
//    cum+=Math.log(n);
//    return cum;
    double suspecion = 0.25;
    if (distance >=1){
      return Math.log(suspecion)*distance;
    }else{
      return Math.log(1-suspecion);
    }
  }
}
