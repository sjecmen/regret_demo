package edu.umich.srg.marketsim;

import com.google.common.base.Objects;
import com.google.common.primitives.Longs;

import java.io.Serializable;

/**
 * The TimeStamp class is just a wrapper around java.lang.Long. This *must* remain an immutable
 * object.
 */
public class TimeStamp implements Comparable<TimeStamp>, Serializable {

  public static final TimeStamp ZERO = new TimeStamp(0);
  public static final TimeStamp INF = new TimeStamp(Long.MAX_VALUE);

  private final long ticks;

  private TimeStamp(long ticks) {
    this.ticks = ticks;
  }

  public static TimeStamp of(long ticks) {
    return new TimeStamp(ticks);
  }

  public long get() {
    return ticks;
  }

  @Override
  public int compareTo(TimeStamp other) {
    return Longs.compare(ticks, other.ticks);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof TimeStamp)) {
      return false;
    } else {
      TimeStamp that = (TimeStamp) other;
      return this.ticks == that.ticks;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(ticks);
  }

  @Override
  public String toString() {
    return ticks + "t";
  }

  private static final long serialVersionUID = -2109498445060507654L;

}
