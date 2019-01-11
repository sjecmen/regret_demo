package edu.umich.srg.util;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.math.BigDecimal;
import java.util.Random;
import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public class SummStatsTest {

  private static final Random rand = new Random();
  private static final double eps = 1e-8;

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  @Test
  @Repeat(100)
  public void positiveTest() {
    SummStats test = SummStats.empty();
    BigStat truth = new BigStat();
    int n = rand.nextInt(1000) + 1000;
    for (int i = 0; i < n; ++i) {
      double d = rand.nextDouble();
      test.accept(d);
      truth.accept(d);
    }
    assertEquals(truth.getSum(), test.getSum(), eps);
    assertEquals(truth.getAverage(), test.getAverage(), eps);
    assertEquals(truth.getVariance(), test.getVariance(), eps);
    assertEquals(truth.getStandardDeviation(), test.getStandardDeviation(), eps);
  }

  @Test
  public void negativeTest() {
    SummStats test = SummStats.empty();
    BigStat truth = new BigStat();
    int n = rand.nextInt(1000) + 1000;
    for (int i = 0; i < n; ++i) {
      double d = -rand.nextDouble();
      test.accept(d);
      truth.accept(d);
    }
    assertEquals(truth.getSum(), test.getSum(), eps);
    assertEquals(truth.getAverage(), test.getAverage(), eps);
    assertEquals(truth.getVariance(), test.getVariance(), eps);
    assertEquals(truth.getStandardDeviation(), test.getStandardDeviation(), eps);
  }

  @Test
  @Repeat(100)
  public void mixedTest() {
    SummStats test = SummStats.empty();
    BigStat truth = new BigStat();
    int n = rand.nextInt(1000) + 1000;
    for (int i = 0; i < n; ++i) {
      double d = rand.nextDouble() - .5;
      test.accept(d);
      truth.accept(d);
    }
    assertEquals(truth.getSum(), test.getSum(), eps);
    assertEquals(truth.getAverage(), test.getAverage(), eps);
    assertEquals(truth.getVariance(), test.getVariance(), eps);
    assertEquals(truth.getStandardDeviation(), test.getStandardDeviation(), eps);
  }

  @Test
  public void emptyTest() {
    SummStats test = SummStats.empty();
    assertEquals(Double.NaN, test.getSum(), eps);
    assertEquals(Double.NaN, test.getAverage(), eps);
    assertEquals(Double.NaN, test.getVariance(), eps);
    assertEquals(Double.NaN, test.getStandardDeviation(), eps);
    assertEquals(Double.NaN, test.getMin(), eps);
    assertEquals(Double.NaN, test.getMax(), eps);
  }

  @Test
  public void singleTest() {
    SummStats test = SummStats.over(DoubleStream.of(5));
    assertEquals(5, test.getSum(), eps);
    assertEquals(5, test.getAverage(), eps);
    assertEquals(0, test.getVariance(), eps);
    assertEquals(0, test.getStandardDeviation(), eps);
    assertEquals(5, test.getMin(), eps);
    assertEquals(5, test.getMax(), eps);
  }

  @Test
  @Repeat(100)
  public void singletonMergeTest() {
    SummStats test = SummStats.empty();
    BigStat truth = new BigStat();
    int n = rand.nextInt(1000) + 1000;
    for (int i = 0; i < n; ++i) {
      double d = rand.nextDouble() - .5;
      test.combine(SummStats.over(DoubleStream.of(d)));
      truth.accept(d);
    }
    assertEquals(truth.getSum(), test.getSum(), eps);
    assertEquals(truth.getAverage(), test.getAverage(), eps);
    assertEquals(truth.getVariance(), test.getVariance(), eps);
    assertEquals(truth.getStandardDeviation(), test.getStandardDeviation(), eps);
  }

  @Test
  @Repeat(100)
  public void multipleMergeTest() {
    SummStats test = SummStats.empty();
    BigStat truth = new BigStat();
    int n = rand.nextInt(1000) + 1000;
    for (int i = 0; i < n; ++i) {
      SummStats sub = SummStats.empty();
      int m = rand.nextInt(10) + 1;
      for (int j = 0; j < m; ++j) {
        double d = rand.nextDouble() - .5;
        sub.accept(d);
        truth.accept(d);
      }
      test.combine(sub);
    }
    assertEquals(truth.getSum(), test.getSum(), eps);
    assertEquals(truth.getAverage(), test.getAverage(), eps);
    assertEquals(truth.getVariance(), test.getVariance(), eps);
    assertEquals(truth.getStandardDeviation(), test.getStandardDeviation(), eps);
  }

  private static class BigStat implements DoubleConsumer {

    private long n;
    private BigDecimal sum, sumsq;

    private BigStat() {
      n = 0;
      sum = new BigDecimal(0);
      sumsq = new BigDecimal(0);
    }

    @Override
    public void accept(double val) {
      ++n;
      sum = sum.add(new BigDecimal(val));
      sumsq = sumsq.add(new BigDecimal(val).pow(2));
    }

    public double getSum() {
      return sum.doubleValue();
    }

    public double getAverage() {
      return sum.doubleValue() / n;
    }

    public double getVariance() {
      return sumsq.multiply(new BigDecimal(n)).subtract(sum.pow(2)).doubleValue() / (n * (n - 1));
    }

    public double getStandardDeviation() {
      return Math.sqrt(getVariance());
    }

  }

}
