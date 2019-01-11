package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse.Entry;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

import java.util.Iterator;

/**
 * Class to store and compute a stochastic process used as a base to determine the private
 * valuations of agents.
 */
public interface Fundamental {

  Price getValueAt(TimeStamp time);

  double rmsd(Iterator<? extends Entry<? extends Number>> prices, TimeStamp finalTime);

  default double rmsd(Iterable<? extends Entry<? extends Number>> prices, TimeStamp finalTime) {
    return rmsd(prices.iterator(), finalTime);
  }

  Iterable<Entry<Number>> getFundamentalValues(TimeStamp finalTime);

}
