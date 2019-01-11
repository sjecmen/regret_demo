package edu.umich.srg.marketsim.privatevalue;

import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static java.util.Objects.requireNonNull;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.fourheap.Order.OrderType;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 * This class allows storage of an agent's private value. In general it is constructed by passing it
 * a descending array of doubles. Each element is the marginal benefit of buying one more unit. The
 * element at `offset` (which defaults to length / 2) is defined as the marginal benefit to buying
 * at position 0. The benefit of buying one unit at position 1 is in index `offset + 1` etc. Selling
 * is inferred as the opposite of buying. Selling at position 1 has the opposite value as buying as
 * position 0. Beyond the extents of the array, the marginal value is inferred as the last marginal
 * value, with the exception that the marginal value must plateau. So if the last element in the
 * array is positive, the marginal value for buying beyond that will be 0 instead of the positive
 * value.
 */
class ListPrivateValue implements PrivateValue {

  private final int offset;
  private final double[] values;
  private final double extraBuy; // Benefit of buying one for large indices
  private final double extraSell; // Benefit of buyong one for small indices

  ListPrivateValue(double[] values, int offset) {
    assert isDecreasing(values);
    this.values = values;
    this.offset = offset;

    if (values.length == 0) {
      extraBuy = 0;
      extraSell = 0;
    } else {
      extraBuy = Math.min(values[values.length - 1], 0);
      extraSell = Math.max(values[0], 0);
    }
  }

  ListPrivateValue(double[] values) {
    this(values, values.length / 2);
  }

  ListPrivateValue(DoubleDistribution dist, int maxPosition, Random rand) {
    this(DoubleStream.generate(() -> -dist.sample(rand)).limit(2 * maxPosition).sorted()
        .map(x -> -x).toArray(), maxPosition);
  }

  @Override
  public double valueForExchange(int position, OrderType type) {
    int index = position + offset - (requireNonNull(type) == SELL ? 1 : 0);
    if (index >= values.length) {
      return extraBuy * type.sign();
    } else if (index < 0) {
      return extraSell * type.sign();
    } else {
      return values[index] * type.sign();
    }
  }

  @Override
  public double valueAtPosition(int position) {
    double value = 0;
    position += offset;
    if (position > offset) {
      int index = Math.min(position, values.length);
      value += (position - index) * extraBuy;
      value += Arrays.stream(values, offset, index).sum();
    } else {
      int index = Math.max(0, position);
      value += (position - index) * extraSell;
      value -= Arrays.stream(values, index, offset).sum();
    }
    return value;
  }

  /** Verifies an array of doubles is decreasing. */
  private static boolean isDecreasing(double[] values) {
    double last = Double.POSITIVE_INFINITY;
    for (double d : values) {
      if (d > last) {
        return false;
      }
      last = d;
    }
    return true;
  }

}
