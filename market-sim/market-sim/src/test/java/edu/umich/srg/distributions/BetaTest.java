package edu.umich.srg.distributions;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.util.SummStats;

import java.util.Random;
import java.util.stream.DoubleStream;

@RunWith(Theories.class)
public class BetaTest {

  private static Random rand = new Random();

  @Ignore // This test is random and won't always be successful
  @Theory
  public void summaryStatisticsTest(@TestDoubles({1, 10, 16}) double alpha) {
    Beta dist = Beta.with(alpha, alpha);
    SummStats stats = SummStats.over(DoubleStream.generate(() -> dist.sample(rand)).limit(100000));
    assertEquals(0.5, stats.getAverage(), 1e-2);
    assertEquals(1 / (8 * alpha + 4), stats.getVariance(), 1e-2);
  }

}
