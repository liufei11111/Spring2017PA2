package edu.stanford.cs276.util;

import java.util.Comparator;

/**
 * Created by feiliu on 5/2/17.
 */
public class StringDoublePairAscendingComparator implements Comparator<Pair<String, Double>> {
  @Override
  public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
    return o2.getSecond().compareTo(o1.getSecond());
  }
}
