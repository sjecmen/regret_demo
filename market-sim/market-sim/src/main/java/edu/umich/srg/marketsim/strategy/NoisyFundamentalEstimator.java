package edu.umich.srg.marketsim.strategy;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;

public abstract class NoisyFundamentalEstimator {

  /**
   * Create a noisy markov fundamental estimator.
   *
   * @param observationVariance The actual observation variance of the fundamental.
   * @param transactionVariance The assumed variance of price distribution from around the
   *        fundamental.
   */
  public static NoisyFundamentalEstimator create(long simLength, double mean, double meanReversion,
      double shockVariance, double observationVariance, double transactionVariance) {
    if (shockVariance == 0) {
      return new ConstantEstimator(mean);
    } else if (meanReversion == 0) {
      return new RandomWalk(mean, shockVariance, observationVariance, transactionVariance);
    } else {
      return new MeanReverting(simLength, mean, meanReversion, shockVariance, observationVariance,
          transactionVariance);
    }
  }

  public abstract double getCurrentEstimate();

  public abstract double estimate();

  public abstract void addFundamentalObservation(TimeStamp currentTime, Price observation);

  public abstract void addTransactionObservation(TimeStamp currentTime, Price observation,
      int quantity);

  private static class ConstantEstimator extends NoisyFundamentalEstimator {

    private final double estimate;

    private ConstantEstimator(double estimate) {
      this.estimate = estimate;
    }

    public double estimate() {
      return estimate;
    }

    public void addFundamentalObservation(TimeStamp currentTime, Price observation) {}

    public void addTransactionObservation(TimeStamp currentTime, Price observation, int quantity) {}

    @Override
    public double getCurrentEstimate() {
      return estimate;
    }

  }

  private abstract static class VarianceEstimator extends NoisyFundamentalEstimator {

    long lastUpdate;
    private final double observationVariance;
    private final double transactionVariance;
    double posteriorMean;
    double posteriorVariance;

    private VarianceEstimator(double initialMean, double observationVariance,
        double transactionVariance) {
      this.observationVariance = observationVariance;
      this.transactionVariance = transactionVariance;
      this.posteriorMean = initialMean;
      this.lastUpdate = 0;
      this.posteriorVariance = 0;
    }

    @Override
    public void addFundamentalObservation(TimeStamp currentTime, Price observation) {
      updateTime(currentTime.get());
      observationUpdate(observation.doubleValue(), observationVariance, 1);
    }

    @Override
    public void addTransactionObservation(TimeStamp currentTime, Price observation, int quantity) {
      updateTime(currentTime.get());
      observationUpdate(observation.doubleValue(), transactionVariance, quantity);
    }

    private void observationUpdate(double observation, double variance, int times) {
      if (variance == 0) {
        posteriorMean = observation;
        posteriorVariance = 0;
      } else if (variance != Double.POSITIVE_INFINITY) {
        // No update for infinite variance
        double varToPower = Math.pow(variance, times - 1);
        posteriorMean = (posteriorMean * variance * varToPower
            + times * observation * varToPower * posteriorVariance)
            / (varToPower * variance + times * varToPower * posteriorVariance);
        posteriorVariance = posteriorVariance * varToPower * variance
            / (varToPower * variance + times * varToPower * posteriorVariance);
      }
    }

    abstract void updateTime(long currentTime);

  }

  private static class RandomWalk extends VarianceEstimator {

    private final double shockVariance;

    private RandomWalk(double mean, double shockVariance, double observationVariance,
        double transactionVariance) {
      super(mean, observationVariance, transactionVariance);
      this.shockVariance = shockVariance;
    }

    @Override
    void updateTime(long currentTime) {
      if (currentTime > lastUpdate) {
        long deltat = currentTime - lastUpdate;
        posteriorVariance += deltat * shockVariance;
        lastUpdate = currentTime;
      }
    }

    @Override
    public double estimate() {
      return posteriorMean;
    }

    @Override
    public double getCurrentEstimate() {
      return posteriorMean;
    }

  }

  private static class MeanReverting extends VarianceEstimator {

    private final long simLength;
    private final double fundamentalMean;
    private final double kappac;
    private final double shockVariance;

    private MeanReverting(long simLength, double mean, double meanReversion, double shockVariance,
        double observationVariance, double transactionVariance) {
      super(mean, observationVariance, transactionVariance);
      this.simLength = simLength;
      this.fundamentalMean = mean;
      this.kappac = 1 - meanReversion;
      this.shockVariance = shockVariance;
    }

    public double getCurrentEstimate() {
      return posteriorMean;
    }

    @Override
    void updateTime(long currentTime) {
      if (currentTime > lastUpdate) {
        long deltat = currentTime - lastUpdate;
        double kappacToPower = Math.pow(kappac, deltat);
        posteriorMean = (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
        posteriorVariance = kappacToPower * kappacToPower * posteriorVariance
            + (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac) * shockVariance;
        lastUpdate = currentTime;
      }
    }

    @Override
    public double estimate() {
      double kappacToPower = Math.pow(kappac, simLength - lastUpdate);
      return (1 - kappacToPower) * fundamentalMean + kappacToPower * posteriorMean;
    }

  }

}
