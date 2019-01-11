package edu.umich.srg.marketsim.strategy;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

public class GaussianFundamentalEstimator {

  private final long simLength;
  private final double mean;
  private final double kappac;

  private GaussianFundamentalEstimator(long simLength, double mean, double meanReversion) {
    this.simLength = simLength;
    this.mean = mean;
    this.kappac = 1 - meanReversion;
  }

  public static GaussianFundamentalEstimator create(long simLength, double mean,
      double meanReversion) {
    return new GaussianFundamentalEstimator(simLength, mean, meanReversion);
  }

  public double estimate(TimeStamp currentTime, Price currentFundamental) {
    double kappacToPower = Math.pow(kappac, simLength - currentTime.get());
    return (1 - kappacToPower) * mean + kappacToPower * currentFundamental.doubleValue();
  }

}
