package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

import java.util.Random;

/** Sample from a gaussian distribution. */
public class Gaussian implements DoubleDistribution {

  private final double mean;
  private final double standardDeviation;

  private Gaussian(double mean, double standardDeviation) {
    this.mean = mean;
    this.standardDeviation = standardDeviation;
  }

  public static Gaussian withMeanVariance(double mean, double variance) {
    return new Gaussian(mean, Math.sqrt(variance));
  }

  public static Gaussian withMeanStandardDeviation(double mean, double standardDeviation) {
    return new Gaussian(mean, standardDeviation);
  }

  @Override
  public double sample(Random rand) {
    return rand.nextGaussian() * standardDeviation + mean;
  }

  public double getMean() {
    return mean;
  }

  public double getStandardDeviation() {
    return standardDeviation;
  }

  public double getVariance() {
    return standardDeviation * standardDeviation;
  }

}
