package edu.umich.srg.marketsim.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestDoubles;
import edu.umich.srg.testing.TestInts;
import edu.umich.srg.util.SummStats;

import java.util.Random;

/*
 * This class has a few tests that take a long time to run, so they shouldn't be run normally. If
 * you run this test, make sure to remove the ignores.
 */
@RunWith(Theories.class)
public class NoisyFundamentalEstimatorTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();
  private static final int times = 1000000;
  private static final double mean = 5e8, shockVariance = 1000, eps = 0.5;

  @Ignore
  @Theory
  public void fundamentalAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance) {
    Gaussian noise = Gaussian.withMeanVariance(0, observationVariance);

    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();
    OptimalLinearError optimal = new OptimalLinearError();

    for (int i = 0; i < times; ++i) {
      Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, shockVariance, 1);
      NoisyFundamentalEstimator estimator = NoisyFundamentalEstimator.create(finalTime, mean,
          meanReversion, shockVariance, observationVariance, 1);

      double actual = fund.getValueAt(TimeStamp.of(finalTime)).doubleValue();
      Price observation = Price.of(actual + noise.sample(rand));
      estimator.addFundamentalObservation(TimeStamp.of(finalTime), observation);
      double estimate = estimator.estimate();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));

      optimal.addObservation(actual, observation.doubleValue());
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
    assertTrue(hmmError.getAverage() > optimal.getError() - eps);
  }

  @Ignore
  @Theory
  public void priceAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double transactionVariance) {
    Gaussian noise = Gaussian.withMeanVariance(0, transactionVariance);

    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();
    OptimalLinearError optimal = new OptimalLinearError();

    for (int i = 0; i < times; ++i) {
      Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, shockVariance, 1);
      NoisyFundamentalEstimator estimator = NoisyFundamentalEstimator.create(finalTime, mean,
          meanReversion, shockVariance, 1, transactionVariance);

      double actual = fund.getValueAt(TimeStamp.of(finalTime)).doubleValue();
      Price observation = Price.of(actual + noise.sample(rand));
      estimator.addTransactionObservation(TimeStamp.of(finalTime), observation, 1);
      double estimate = estimator.estimate();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));

      optimal.addObservation(actual, observation.doubleValue());
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
    assertTrue(hmmError.getAverage() > optimal.getError() - eps);
  }

  @Ignore
  @Theory
  public void multiSampleTest(@TestInts({1, 2}) int tstep, @TestInts({2, 5}) int numSteps,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance) {
    Gaussian noise = Gaussian.withMeanVariance(0, observationVariance);

    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty();

    for (int i = 0; i < times; ++i) {
      Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, shockVariance, 1);
      NoisyFundamentalEstimator estimator = NoisyFundamentalEstimator.create(tstep * numSteps, mean,
          meanReversion, shockVariance, observationVariance, 1);

      double observations[] = new double[numSteps + 1];
      observations[0] = 1;

      double actual = 0;
      int time = 0;
      for (int j = 0; j < numSteps; ++j) {
        time += tstep;

        actual = fund.getValueAt(TimeStamp.of(time)).doubleValue();
        Price observation = Price.of(actual + noise.sample(rand));
        estimator.addFundamentalObservation(TimeStamp.of(time), observation);
        observations[j + 1] = observation.doubleValue();
      }

      double estimate = estimator.estimate();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
  }

  @Ignore
  @Theory
  public void multipleFundamentalAccuracyTest(@TestInts({1, 4}) int finalTime,
      @TestDoubles({0, 0.5, 1}) double meanReversion,
      @TestDoubles({0, 100}) double observationVariance, @TestInts({2}) int numSamples) {
    Gaussian noise = Gaussian.withMeanVariance(0, observationVariance);

    SummStats naiveBias = SummStats.empty(), naiveError = SummStats.empty(),
        hmmBias = SummStats.empty(), hmmError = SummStats.empty(), baselineBias = SummStats.empty(),
        baselineError = SummStats.empty();

    for (int i = 0; i < times; ++i) {
      Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, shockVariance, 1);
      NoisyFundamentalEstimator estimator =
          NoisyFundamentalEstimator.create(finalTime, mean, meanReversion, shockVariance,
              observationVariance, 1),
          baseline = NoisyFundamentalEstimator.create(finalTime, mean, meanReversion, shockVariance,
              observationVariance, 1);

      double actual = fund.getValueAt(TimeStamp.of(finalTime)).doubleValue();

      Price observation = null;
      for (int j = 0; j < numSamples; ++j) {
        observation = Price.of(actual + noise.sample(rand));
        estimator.addFundamentalObservation(TimeStamp.of(finalTime), observation);
      }
      baseline.addFundamentalObservation(TimeStamp.of(finalTime), observation);

      double estimate = estimator.estimate(), baselineEstimate = baseline.estimate();

      naiveBias.accept(mean - actual);
      hmmBias.accept(estimate - actual);
      baselineBias.accept(baselineEstimate - actual);

      naiveError.accept((mean - actual) * (mean - actual));
      hmmError.accept((estimate - actual) * (estimate - actual));
      baselineError.accept((baselineEstimate - actual) * (baselineEstimate - actual));
    }

    assertEquals(0, naiveBias.getAverage(), eps);
    assertEquals(0, hmmBias.getAverage(), eps);
    assertEquals(0, baselineBias.getAverage(), eps);
    assertTrue(naiveError.getAverage() > hmmError.getAverage() - eps);
    assertTrue(baselineError.getAverage() > hmmError.getAverage() - eps);
  }

  @Repeat(100)
  @Theory
  public void infiniteObservationVarianceTest(@TestDoubles({0, 0.2, 1}) double meanReversion,
      @TestDoubles({0, 100}) double shockVariance) {
    NoisyFundamentalEstimator estimator = NoisyFundamentalEstimator.create(100, mean, meanReversion,
        shockVariance, Double.POSITIVE_INFINITY, 0);
    long price = Math.abs(rand.nextLong());
    estimator.addFundamentalObservation(TimeStamp.of(10), Price.of(price));
    assertEquals(Price.of(mean), Price.of(estimator.estimate()));
  }

  @Repeat(100)
  @Theory
  public void zeroObservationVarianceTest(@TestDoubles({0, 0.2, 1}) double meanReversion,
      @TestDoubles({0, 100}) double shockVariance) {
    long simLength = 100;

    Fundamental fund = GaussianMeanReverting.create(rand, mean, meanReversion, shockVariance, 1);
    NoisyFundamentalEstimator estimator =
        NoisyFundamentalEstimator.create(simLength, mean, meanReversion, shockVariance, 0, 0);
    GaussianFundamentalEstimator groundTruth =
        GaussianFundamentalEstimator.create(simLength, mean, meanReversion);

    IntUniform nextTime = Uniform.closed(1, 3);
    int time = 0;
    while (time < simLength) {
      TimeStamp t = TimeStamp.of(time);

      Price obs = fund.getValueAt(t);
      estimator.addFundamentalObservation(t, obs);
      assertEquals(groundTruth.estimate(t, obs), estimator.estimate(), eps);

      time += nextTime.sample(rand);
    }
  }

  private static class OptimalLinearError {
    private int count = 0;
    private double featureMean = 0, targetMean = 0, featureError = 0, targetError = 0,
        featureTargetError = 0;

    public void addObservation(double target, double feature) {
      ++count;
      double featureDelta = feature - featureMean, targetDelta = target - targetMean;
      featureMean += featureDelta / count;
      featureError += featureDelta * (feature - featureMean);
      featureTargetError += (feature - featureMean) * targetDelta;
      targetMean += targetDelta / count;
      targetError += targetDelta * (target - targetMean);
    }

    public double getError() {
      double slope = featureTargetError / featureError,
          intercept = targetMean - slope * featureMean;
      return slope * slope * featureError / count + intercept * intercept + targetError / count
          + 2 * slope * intercept * featureMean - 2 * intercept * targetMean
          - 2 * slope * featureTargetError / count;
    }

  }

}
