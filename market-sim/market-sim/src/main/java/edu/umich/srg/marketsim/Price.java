package edu.umich.srg.marketsim;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.math.RoundingMode.HALF_EVEN;

import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import com.google.common.math.DoubleMath;
import com.google.common.primitives.Ints;

import java.io.Serializable;

/**
 * Price class is wrapper for long; one unit represents one thousandth of a dollar.
 * 
 * @author ewah
 */
public class Price extends Number implements Comparable<Price>, Serializable {

  protected static final Ordering<Price> ord = Ordering.natural();

  public static final Price INF = new Price(Integer.MAX_VALUE) {
    private static final long serialVersionUID = 1849387089333514388L;

    @Override
    public float floatValue() {
      return Float.POSITIVE_INFINITY;
    }

    @Override
    public double doubleValue() {
      return Double.POSITIVE_INFINITY;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "$" + doubleValue();
    }
  };
  public static final Price NEG_INF = new Price(Integer.MIN_VALUE) {
    private static final long serialVersionUID = -2568290536011656239L;

    @Override
    public float floatValue() {
      return Float.NEGATIVE_INFINITY;
    }

    @Override
    public double doubleValue() {
      return Double.NEGATIVE_INFINITY;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "$" + doubleValue();
    }
  };
  public static final Price ZERO = new Price(0);

  protected final long ticks;

  public Price(long ticks) {
    this.ticks = ticks;
  }

  public static Price of(long ticks) {
    return new Price(ticks);
  }

  /**
   * Constructor for a Price object. Price is rounded to the nearest long using the HALF_EVEN
   * method. This may return INF or NEG_INF if the input was too high.
   */
  public static Price of(double ticks) {
    checkArgument(!Double.isNaN(ticks));
    if (ticks > Long.MAX_VALUE) {
      return INF;
    } else if (ticks < Long.MIN_VALUE) {
      return NEG_INF;
    } else {
      return new Price(DoubleMath.roundToLong(ticks, HALF_EVEN));
    }
  }

  @Override
  public int intValue() {
    return Ints.saturatedCast(ticks);
  }

  @Override
  public long longValue() {
    return ticks;
  }

  @Override
  public float floatValue() {
    return ticks;
  }

  @Override
  public double doubleValue() {
    return ticks;
  }

  /**
   * Return 0 if price is negative
   * 
   * @return Non-negative version of the price.
   */
  public Price nonnegative() {
    return ord.max(this, ZERO);
  }

  @Override
  public int compareTo(Price price) {
    checkNotNull(price);
    if (this == INF) {
      return price == INF ? 0 : 1;
    } else if (this == NEG_INF) {
      return price == NEG_INF ? 0 : 1;
    } else {
      return Long.compare(ticks, price.ticks);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ticks);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Price)) {
      return false;
    } else {
      Price that = (Price) other;
      if (this == INF) {
        return that == INF;
      } else if (this == NEG_INF) {
        return that == NEG_INF;
      } else {
        return this.ticks == that.ticks;
      }
    }
  }

  @Override
  public String toString() {
    return "$" + ticks;
  }

  private static final long serialVersionUID = 772101228717034473L;

}
