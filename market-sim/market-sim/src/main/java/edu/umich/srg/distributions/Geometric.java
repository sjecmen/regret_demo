package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.util.Random;

/** Geometric distribution that samples between 0 and Long.MAX_VALUE. */
public class Geometric implements LongDistribution {

  private final double weight;

  private Geometric(double successProbability) {
    this.weight = Math.log1p(-successProbability);
  }

  public static Geometric withSuccessProbability(double successProb) {
    return new Geometric(successProb);
  }

  @Override
  public long sample(Random rand) {
    return (long) (Math.log1p(-rand.nextDouble()) / weight);
  }

}
