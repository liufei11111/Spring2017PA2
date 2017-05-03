package edu.stanford.cs276.util;

import java.util.Comparator;

/**
 * Created by feiliu on 5/1/17.
 */
public class IntegerDoublePairDecendingComparator implements Comparator<Pair<Integer,Double>> {

  @Override
  public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
    return o1.getSecond().compareTo(o2.getSecond());
  }
}
