package edu.umich.srg.util;

import java.util.PrimitiveIterator.OfDouble;
import java.util.function.DoubleUnaryOperator;

public final class Linear {

  /** Perform a linear fir on parallel data. */
  public static DoubleUnaryOperator linearFit(OfDouble independent, OfDouble dependent) {
    CovarStats cs = CovarStats.over(independent, dependent);
    double slope = cs.getCovariance() / cs.getXVariance();
    double intercept = cs.getYAverage() - slope * cs.getXAverage();
    return d -> d * slope + intercept;
  }

  /** Find the rsquared on linear data. */
  public static double pearsonCoefficient(OfDouble independent, OfDouble dependent) {
    CovarStats cs = CovarStats.over(independent, dependent);
    return cs.getCovariance() * cs.getCovariance() / (cs.getXVariance() * cs.getYVariance());
  }

  private Linear() {} // Unconstructable

}
