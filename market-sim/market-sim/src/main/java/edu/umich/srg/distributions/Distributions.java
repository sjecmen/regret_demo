package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.distributions.Distribution.IntDistribution;
import edu.umich.srg.distributions.Distribution.LongDistribution;

import java.util.Random;
import java.util.function.Function;

public final class Distributions {

  /** Box an int distribution. */
  public static Distribution<Integer> box(IntDistribution dist) {
    return new Distribution<Integer>() {
      @Override
      public Integer sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  /** Box a long distribution. */
  public static Distribution<Long> box(LongDistribution dist) {
    return new Distribution<Long>() {
      @Override
      public Long sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  /** Box a double distribution. */
  public static Distribution<Double> box(DoubleDistribution dist) {
    return new Distribution<Double>() {
      @Override
      public Double sample(Random rand) {
        return dist.sample(rand);
      }
    };
  }

  /** map over distribution outputs. */
  public static <T, R> Distribution<R> map(Distribution<T> initial, Function<T, R> func) {
    return new Distribution<R>() {
      @Override
      public R sample(Random rand) {
        return func.apply(initial.sample(rand));
      }
    };
  }

  private Distributions() {} // Unconstructable

}
