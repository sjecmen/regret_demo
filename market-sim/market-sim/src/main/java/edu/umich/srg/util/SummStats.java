package edu.umich.srg.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Multiset.Entry;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

/**
 * Compact Summary Statistics Class meant to only calculate sum and standard deviation. This
 * generally has similar accuracy than the Apache Commons Math SummaryStatistics class, but a better
 * API. Unlike the DoubleSummaryStatistics class provided by java, this also calculated squared
 * error and resulting statistics.
 */
public class SummStats implements DoubleConsumer, Consumer<Entry<? extends Number>> {

  private long count;
  private double average;
  private double squaredError;
  private double min;
  private double max;

  private SummStats(long count, double mean, double squaredError, double min, double max) {
    this.count = count;
    this.average = mean;
    this.squaredError = squaredError;
    this.min = min;
    this.max = max;
  }

  /** Create a new SumStats object with no data. */
  public static SummStats empty() {
    return new SummStats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
  }

  public static SummStats over(DoubleStream stream) {
    return stream.collect(SummStats::empty, SummStats::accept, SummStats::combine);
  }

  public static SummStats over(Collection<? extends Entry<? extends Number>> data) {
    return data.stream().collect(SummStats::empty, SummStats::accept, SummStats::combine);
  }

  public static SummStats over(double... data) {
    return over(Arrays.stream(data));
  }

  @Override
  public void accept(double value) {
    count += 1;
    double delta = value - average;
    average += delta / count;
    squaredError += delta * (value - average);
    min = Math.min(min, value);
    max = Math.max(max, value);
  }

  @Override
  public void accept(Entry<? extends Number> entry) {
    acceptNTimes(entry.getElement().doubleValue(), entry.getCount());
  }

  /** Add several data points. */
  public void accept(Iterable<? extends Number> values) {
    for (Number n : values) {
      accept(n.doubleValue());
    }
  }

  /** Add several data points. */
  public void accept(OfDouble values) {
    while (values.hasNext()) {
      accept(values.nextDouble());
    }
  }

  /** Accept a data point several times. This is more efficient than calling accept n times. */
  public void acceptNTimes(double value, long times) {
    checkArgument(times >= 0);
    if (times > 0) {
      combine(new SummStats(times, value, 0, value, value));
    }
  }

  /** Return the mean of all data added so far. */
  public double getAverage() {
    return count == 0 ? Double.NaN : average;
  }

  /** Return the sample variance of all data added so far. */
  public double getVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : squaredError / (count - 1);
  }

  /** Return the sample standard deviation of all data added so far. */
  public double getStandardDeviation() {
    return Math.sqrt(getVariance());
  }

  public double getSum() {
    return getAverage() * count;
  }

  public double getMin() {
    return count == 0 ? Double.NaN : min;
  }

  public double getMax() {
    return count == 0 ? Double.NaN : max;
  }

  /** Return the number of data points. */
  public long getCount() {
    return count;
  }

  /** Merge other values into this. */
  public SummStats combine(SummStats other) {
    count += other.count;
    double delta = other.average - average;
    // XXX not certain of proper order of operations to minimize precision loss
    average += delta * other.count / count;
    squaredError += other.squaredError + delta * (other.average - average) * other.count;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof SummStats)) {
      return false;
    } else {
      SummStats that = (SummStats) other;
      return this.count == that.count && this.average == that.average && this.max == that.max
          && this.min == that.min && this.squaredError == that.squaredError;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, min, max, average, squaredError);
  }

  @Override
  public String toString() {
    return "<n: " + count + ", mean: " + average + ">";
  }

}
