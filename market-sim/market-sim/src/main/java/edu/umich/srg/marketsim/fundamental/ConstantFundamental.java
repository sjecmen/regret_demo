package edu.umich.srg.marketsim.fundamental;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.collect.Sparse.Entry;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.SummStats;

import java.util.Collections;
import java.util.Iterator;

public class ConstantFundamental extends GaussianMeanReverting {

  private final Price constant;

  private ConstantFundamental(Price constant) {
    this.constant = constant;
  }

  public static ConstantFundamental create(Price constant) {
    return new ConstantFundamental(constant);
  }

  public static ConstantFundamental create(Number constant) {
    return new ConstantFundamental(Price.of(constant.doubleValue()));
  }

  @Override
  public Price getValueAt(TimeStamp time) {
    return constant;
  }

  @Override
  public Iterable<Entry<Number>> getFundamentalValues(TimeStamp finalTime) {
    return Collections.singleton(Sparse.immutableEntry(0, constant));
  }

  @Override
  public double rmsd(Iterator<? extends Entry<? extends Number>> prices, TimeStamp finalTime) {
    if (!prices.hasNext()) {
      return Double.NaN;
    }
    long longTime = finalTime.get();
    SummStats rmsd = SummStats.empty();
    Entry<? extends Number> nextPrice;
    Entry<? extends Number> lastPrice = prices.next();
    while (prices.hasNext() && (nextPrice = prices.next()).getIndex() <= longTime) {
      double diff = lastPrice.getElement().doubleValue() - constant.doubleValue();
      rmsd.acceptNTimes(diff, nextPrice.getIndex() - lastPrice.getIndex());
      lastPrice = nextPrice;
    }
    double diff = lastPrice.getElement().doubleValue() - constant.doubleValue();
    rmsd.acceptNTimes(diff, longTime - lastPrice.getIndex() + 1);
    return Math.sqrt(rmsd.getAverage());
  }

  private static final long serialVersionUID = 1;

}
