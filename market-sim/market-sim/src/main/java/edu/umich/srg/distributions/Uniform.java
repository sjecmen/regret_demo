package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.math.LongMath;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** A class for constructing uniform distributions. */
public abstract class Uniform<T> implements Distribution<T> {

  /** Create a uniform distribution over several options. */
  public static <T> Uniform<T> over(Iterator<? extends T> options) {
    return over(ImmutableList.copyOf(options));
  }

  /** Create a uniform distribution over several options. */
  public static <T> Uniform<T> over(Iterable<? extends T> options) {
    ImmutableList<? extends T> listOptions = ImmutableList.copyOf(options);
    checkArgument(listOptions.size() > 0);
    if (listOptions.size() == 1) {
      return new ConstantUniform<>(listOptions.get(0));
    } else {
      return new ListUniform<>(listOptions);
    }
  }

  /** Create a uniform distribution over several options. */
  public static <T> Uniform<T> over(@SuppressWarnings("unchecked") T... options) {
    return over(ImmutableList.copyOf(options));
  }



  // Int

  public static IntUniform closed(int minInclusive, int maxInclusive) {
    return new IntUniform(minInclusive, BoundType.CLOSED, maxInclusive, BoundType.CLOSED);
  }

  // Long

  public static LongUniform closed(long minInclusive, long maxInclusive) {
    return new LongUniform(minInclusive, BoundType.CLOSED, maxInclusive, BoundType.CLOSED);
  }

  public static IntUniform closedOpen(int minInclusive, int maxExclusive) {
    return new IntUniform(minInclusive, BoundType.CLOSED, maxExclusive, BoundType.OPEN);
  }

  // Long

  public static LongUniform closedOpen(long minInclusive, long maxExclusive) {
    return new LongUniform(minInclusive, BoundType.CLOSED, maxExclusive, BoundType.OPEN);
  }

  // Double

  public static ContinuousUniform closedOpen(double minInclusive, double maxExclusive) {
    return new ContinuousUniform(minInclusive, maxExclusive);
  }

  public static IntUniform openClosed(int minExclusive, int maxInclusive) {
    return new IntUniform(minExclusive, BoundType.OPEN, maxInclusive, BoundType.CLOSED);
  }

  // Long

  public static LongUniform openClosed(long minExclusive, long maxInclusive) {
    return new LongUniform(minExclusive, BoundType.OPEN, maxInclusive, BoundType.CLOSED);
  }

  public static IntUniform open(int minExclusive, int maxExclusive) {
    return new IntUniform(minExclusive, BoundType.OPEN, maxExclusive, BoundType.OPEN);
  }

  // Long

  public static LongUniform open(long minExclusive, long maxExclusive) {
    return new LongUniform(minExclusive, BoundType.OPEN, maxExclusive, BoundType.OPEN);
  }

  // Double

  public static ContinuousUniform continuous() {
    return new ContinuousUniform(0, 1);
  }

  private static class ConstantUniform<T> extends Uniform<T> {

    private final T option;

    private ConstantUniform(T option) {
      this.option = option;
    }

    @Override
    public T sample(Random rand) {
      return option;
    }

  }

  private static class ListUniform<T> extends Uniform<T> {

    private final List<? extends T> options;

    private ListUniform(ImmutableList<? extends T> options) {
      this.options = options;
    }

    @Override
    public T sample(Random rand) {
      return options.get(rand.nextInt(options.size()));
    }

  }

  public static class IntUniform implements IntDistribution {
    // FIXME this will fail if the range is larger than Integer.MAX_VALUE;
    private final int range;
    private final int offset;

    private IntUniform(int min, BoundType minBound, int max, BoundType maxBound) {
      this.range =
          max - min - (minBound == BoundType.OPEN ? 1 : 0) + (maxBound == BoundType.CLOSED ? 1 : 0);
      checkArgument(range > 0, "Must have a non zero range to sample from");
      this.offset = min;
    }

    @Override
    public int sample(Random rand) {
      return rand.nextInt(range) + offset;
    }

  }

  // Long

  public static class LongUniform implements LongDistribution {

    private final long min;
    private final long max;
    private final long range;
    private final long offset;

    private LongUniform(long min, BoundType minBound, long max, BoundType maxBound) {
      // TODO Might be able to use unsigned math for this...
      // TODO Actually, this only fails if the difference is create than half of the range, in which
      // case this should just be the range...
      this.range = LongMath.checkedAdd(
          LongMath.checkedSubtract(max, min) - (minBound == BoundType.OPEN ? 1 : 0),
          (maxBound == BoundType.CLOSED ? 1 : 0));
      checkArgument(range > 0, "Must have a non zero range to sample from");
      this.offset = min;
      this.min = Long.MIN_VALUE - (Long.MIN_VALUE % range);
      this.max = Long.MAX_VALUE - (Long.MAX_VALUE % range);
    }

    @Override
    public long sample(Random rand) {
      long result;
      do {
        result = rand.nextLong();
      } while (result < min || result >= max);
      long ret = result % range + offset;
      return ret < 0 ? ret + range : ret;
    }

  }

  // Double

  public static class ContinuousUniform implements DoubleDistribution {

    private final double range;
    private final double offset;

    private ContinuousUniform(double minInclusive, double maxExclusive) {
      this.range = maxExclusive - minInclusive;
      this.offset = minInclusive;
    }

    @Override
    public double sample(Random rand) {
      return rand.nextDouble() * range + offset;
    }

  }

}
