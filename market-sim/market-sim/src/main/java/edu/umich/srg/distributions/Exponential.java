package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

import java.util.Random;

public class Exponential implements DoubleDistribution {

  private double lambda;

  private Exponential(double rate) {
    this.lambda = rate;
  }

  public static Exponential withRate(double rate) {
    return new Exponential(rate);
  }

  @Override
  public double sample(Random rand) {
    return -Math.log1p(-rand.nextDouble()) / lambda;
  }

}
