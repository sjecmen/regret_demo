package edu.umich.srg.marketsim.fundamental;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.collect.Collectors;
import edu.umich.srg.collect.Sparse;
import edu.umich.srg.distributions.Distribution.LongDistribution;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.testing.Asserts;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;
import edu.umich.srg.testing.TestLongs;
import edu.umich.srg.util.SummStats;

import java.util.DoubleSummaryStatistics;
import java.util.Random;
import java.util.stream.Stream;

@RunWith(Theories.class)
public class GaussianMeanRevertingTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int mean = 1000;

  @Repeat(1000)
  @Theory
  public void fundTest(@TestDoubles({0, 0.3, 1}) double kappa,
      @TestDoubles({100, 1000000}) double shockVar,
      @TestDoubles({0, 0.2, 0.8, 1}) double shockProb) {
    TimeStamp t1 = TimeStamp.of(rand.nextInt(100) + 1),
        t2 = TimeStamp.of(t1.get() + rand.nextInt(100) + 1);
    long seed = rand.nextLong();

    Fundamental f1 =
        GaussianMeanReverting.create(new Random(seed), mean, kappa, shockVar, shockProb);
    Price p11 = f1.getValueAt(t1);
    Price p12 = f1.getValueAt(t2);


    Fundamental f2 =
        GaussianMeanReverting.create(new Random(seed), mean, kappa, shockVar, shockProb);
    Price p22 = f2.getValueAt(t2);
    Price p21 = f2.getValueAt(t1);

    assertEquals("First prices were not equal", p11, p21);
    assertEquals("Second prices were not equal", p12, p22);
  }

  /**
   * Tests that it can generate a large fundamental value reasonble quickly. This only really works
   * if there are always jumps, as hypergeometrics are hard to sample from.
   */
  @Test
  public void longFundamentalTest() {
    TimeStamp finalTime = TimeStamp.of(1000000000000L);
    Fundamental fundamental = GaussianMeanReverting.create(rand, mean, 0.5, 100, 1);
    fundamental.getValueAt(finalTime);
    assertEquals(53, Iterables.size(fundamental.getFundamentalValues(finalTime)));
  }

  /**
   * This test uses FundamentalRmsd to calculate the sparse expected rmsd of a fundamental without
   * requesting new values. This rmsd is compared to the true rmsd after sampling the rest of the
   * fundamental. The actual test conditions are poorly constructed, and a true statistical test
   * should actually be made. As it stands the test is random and so might fail so it's ignored by
   * default. Also, this calculation has a slight positive bias, which should probably be corrected
   * for at some point.
   */
  @Ignore("Random test, may not always succeed")
  @Theory
  public void rmsdTest(@TestDoubles({10000}) double mean, @TestDoubles({10000, 0}) double price,
      @TestDoubles({1, 0.01, 0}) double meanReversion, @TestDoubles({100}) double variance,
      @TestLongs({2, 100}) long finalTime, @TestInts({10}) int numSamples) {

    LongDistribution sampling = Uniform.open(0, finalTime);

    DoubleSummaryStatistics[] averages = Stream.generate(() -> {
      Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, variance, 1);
      for (int j = 0; j < numSamples; j++) {
        fund.getValueAt(TimeStamp.of(sampling.sample(rand)));
      }

      double expected = fund.rmsd(ImmutableList.of(Sparse.immutableEntry(0, price)).iterator(),
          TimeStamp.of(finalTime));

      SummStats observedMean = SummStats.empty();
      for (long t = 0; t <= finalTime; t++) {
        double diff = fund.getValueAt(TimeStamp.of(t)).doubleValue() - price;
        observedMean.accept(diff * diff);
      }

      return new double[] {expected, Math.sqrt(observedMean.getAverage())};
    }).parallel().limit(10000).collect(Collectors.arraySummaryStatistics(2));

    double expected = averages[0].getAverage(), observed = averages[1].getAverage(),
        error = Math.abs((observed - expected) / observed);

    Asserts.assertTrue(error < 0.01, "Average error (%f) was too high", error);
  }

}
