package edu.umich.srg.distributions;

import java.util.Random;

/**
 * Interface the represents a distribution that can be sampled from. The source of randomness is
 * independent of the distribution. This distribution just contains the knowledge necessary to
 * produce a sample.
 */
public interface Distribution<T> {

  T sample(Random rand);

  /** A distribution that returns integers. */
  interface IntDistribution {
    int sample(Random rand);
  }

  /** A distribution that returns longs. */
  interface LongDistribution {
    long sample(Random rand);
  }

  /** A distribution that returns doubles. */
  interface DoubleDistribution {
    double sample(Random rand);
  }

}
