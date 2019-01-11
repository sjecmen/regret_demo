package edu.umich.srg.marketsim.privatevalue;

import edu.umich.srg.distributions.Gaussian;

import java.util.Random;

public final class PrivateValues {

  public static PrivateValue gaussianPrivateValue(Random rand, int maxPosition, double variance) {
    return new ListPrivateValue(Gaussian.withMeanVariance(0, variance), maxPosition, rand);
  }

  public static PrivateValue fromMarginalBuys(double[] marginalBuys) {
    return new ListPrivateValue(marginalBuys);
  }

  public static PrivateValue noPrivateValue() {
    return new ListPrivateValue(new double[0]);
  }

  // Unconstructable
  private PrivateValues() {}

}
