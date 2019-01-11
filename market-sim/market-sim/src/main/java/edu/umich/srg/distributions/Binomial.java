package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.io.Serializable;
import java.util.Random;
import java.util.stream.DoubleStream;

/**
 * Generate samples from a Binomail. Method is taken from: "Computer Methods for Sampling from
 * Gamma, Beta, Poisson and Binomial Distributions" - J. H. Ahrens and U. Dieter (1973). It relies
 * on the approximate beta sampling, so is not perfect.
 */
public abstract class Binomial implements LongDistribution, Serializable {

  /** Create a binomial. */
  public static Binomial with(long numDraws, double successProbability) {
    checkArgument(numDraws >= 0, "Can't sample Binomial with less than one trial %d", numDraws);
    checkArgument(0 <= successProbability && successProbability <= 1, "p (%f) must be a probility",
        successProbability);
    return binomialSwitch(numDraws, successProbability);
  }

  /** Constructor without bounds checking. */
  private static Binomial binomialSwitch(long numDraws, double successProbability) {
    if (successProbability == 0 || numDraws == 0) {
      return new ConstantBinomial(0);
    } else if (successProbability == 1) {
      return new ConstantBinomial(numDraws);
    } else if (numDraws < 37) { // Guess on transition point
      return new BernoulliBinomial(numDraws, successProbability);
    } else if (numDraws % 2 == 0) {
      return new EvenBinomial(numDraws, successProbability);
    } else {
      return new BetaBinomial(numDraws, successProbability);
    }
  }

  private static class ConstantBinomial extends Binomial {

    private final long constant;

    private ConstantBinomial(long constant) {
      this.constant = constant;
    }

    @Override
    public long sample(Random rand) {
      return constant;
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * When n is small, a Binomial is best sampled by just generating n Bernoulli trials and summing
   * the successes.
   * 
   * @author erik
   *
   */
  private static class BernoulliBinomial extends Binomial {

    private final long numDraws;
    private final double successProbability;

    private BernoulliBinomial(long numDraws, double successProbability) {
      this.numDraws = numDraws;
      this.successProbability = successProbability;
    }

    @Override
    public long sample(Random rand) {
      return DoubleStream.generate(rand::nextDouble).limit(numDraws)
          .filter(samp -> samp < successProbability).count();
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * Recursive binomial sampling based off of beta sampling distribution. If n is large, then we can
   * use the Beta method for generating binomial samples. The idea of the method is simple. The jth
   * order statistic of n Bernoulli trials is Beta(j, n - j + 1). Thus, if j = (n + 1) / 2 and Y ~
   * Beta(j, j), then if Y < p at least j of the trials have to be larger than p (at least j
   * successes). The conditional distribution for the trials that were less than Y but greater than
   * p is also Binomial, and so we get a recursive definition that takes O(log n).
   */
  private static class BetaBinomial extends Binomial {

    private final long halfDraws;
    private final double successProbability;
    private final Beta orderDistribution;

    private BetaBinomial(long numDraws, double successProbability) {
      this.successProbability = successProbability;
      this.halfDraws = (numDraws + 1) / 2;
      this.orderDistribution = Beta.with(halfDraws, halfDraws);
    }

    @Override
    public long sample(Random rand) {
      double order = orderDistribution.sample(rand);
      if (order < successProbability) {
        return halfDraws + Binomial
            .binomialSwitch(halfDraws - 1, (successProbability - order) / (1 - order)).sample(rand);
      } else {
        return Binomial.binomialSwitch(halfDraws - 1, successProbability / order).sample(rand);
      }
    }

    private static final long serialVersionUID = 1;

  }

  /**
   * The Beta method works best when n is odd, thus if it's even, we do one independent Bernoulli
   * trial, and add its result to the recursive definition.
   */
  private static class EvenBinomial extends Binomial {

    private final double successProbability;
    private final Binomial oddBernoulli;

    private EvenBinomial(long numDraws, double successProbability) {
      this.successProbability = successProbability;
      this.oddBernoulli = Binomial.binomialSwitch(numDraws - 1, successProbability);
    }

    @Override
    public long sample(Random rand) {
      return (rand.nextDouble() < successProbability ? 1 : 0) + oddBernoulli.sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  private Binomial() {} // Unconstructable

  private static final long serialVersionUID = 1;


}
