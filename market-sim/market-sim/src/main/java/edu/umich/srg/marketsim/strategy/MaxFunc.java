package edu.umich.srg.marketsim.strategy;

import org.apache.commons.math3.analysis.UnivariateFunction;

import edu.umich.srg.fourheap.Order.OrderType;

class MaxFunc implements UnivariateFunction {

  private double[] coefficients;
  private double V;
  private OrderType buyOrSell;

  public MaxFunc(double[] coefficients, double V, OrderType buyOrSell) {
    this.coefficients = coefficients;
    this.V = V;
    this.buyOrSell = buyOrSell;
  }

  @Override
  public double value(double x) {
    if (buyOrSell == OrderType.BUY) {
      return (V - x) * (coefficients[0] * x * x * x + coefficients[1] * x * x + coefficients[2] * x
          + coefficients[3]);
    } else {
      return (x - V) * (coefficients[0] * x * x * x + coefficients[1] * x * x + coefficients[2] * x
          + coefficients[3]);
    }
  }
}
