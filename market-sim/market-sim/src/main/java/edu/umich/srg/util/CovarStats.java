package edu.umich.srg.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;
import java.util.PrimitiveIterator.OfDouble;

/**
 * Class for efficient accurate covariance calculation. Uses the covarance version of the knuth
 * method.
 */
public class CovarStats {

  private long count;
  private double averageX;
  private double squaredErrorX;
  private double minX;
  private double maxX;
  private double averageY;
  private double squaredErrorY;
  private double minY;
  private double maxY;
  private double jointSquaredError;

  private CovarStats(long count, double averageX, double squaredErrorX, double minX, double maxX,
      double averageY, double squaredErrorY, double minY, double maxY, double jointSquaredError) {
    this.count = count;
    this.averageX = averageX;
    this.squaredErrorX = squaredErrorX;
    this.minX = minX;
    this.maxX = maxX;
    this.averageY = averageY;
    this.squaredErrorY = squaredErrorY;
    this.minY = minY;
    this.maxY = maxY;
    this.jointSquaredError = jointSquaredError;
  }

  /** Create a new CovarStats object with no data. */
  public static CovarStats empty() {
    return new CovarStats(0, 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0,
        Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0);
  }

  /** Calculate covarance statistics over iterators. */
  public static CovarStats over(OfDouble dataX, OfDouble dataY) {
    CovarStats stats = CovarStats.empty();
    while (dataX.hasNext() && dataY.hasNext()) {
      stats.accept(dataX.nextDouble(), dataY.nextDouble());
    }
    return stats;
  }

  /** Accept a point of x and y data. */
  public void accept(double pointX, double pointY) {
    count += 1;
    final double deltaX = pointX - averageX;
    final double deltaY = pointY - averageY;

    averageX += deltaX / count;
    squaredErrorX += deltaX * (pointX - averageX);
    minX = Math.min(minX, pointX);
    maxX = Math.max(maxX, pointX);

    jointSquaredError += deltaY * (pointX - averageX);

    averageY += deltaY / count;
    squaredErrorY += deltaY * (pointY - averageY);
    minY = Math.min(minY, pointY);
    maxY = Math.max(maxY, pointY);
  }

  /** Accept a point several times. This is more efficient than calling accept n times. */
  public void acceptNTimes(double dataX, double dataY, long times) {
    checkArgument(times >= 0);
    if (times > 0) {
      combine(new CovarStats(times, dataX, 0, dataX, dataX, dataY, 0, dataY, dataY, 0));
    }
  }

  /** Return the mean of all x data added so far. */
  public double getXAverage() {
    return count == 0 ? Double.NaN : averageX;
  }

  public double getYAverage() {
    return count == 0 ? Double.NaN : averageY;
  }

  /** Return the sample variance of all data added so far. */
  public double getXVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : squaredErrorX / (count - 1);
  }

  public double getYVariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : squaredErrorY / (count - 1);
  }

  public double getCovariance() {
    return count == 0 ? Double.NaN : count == 1 ? 0 : jointSquaredError / (count - 1);
  }

  /** Return the sample standard deviation of all data added so far. */
  public double getXStandardDeviation() {
    return Math.sqrt(getXVariance());
  }

  public double getYStandardDeviation() {
    return Math.sqrt(getYVariance());
  }

  public double getCoStandardDeviation() {
    return Math.sqrt(getCovariance());
  }

  public double getXSum() {
    return getXAverage() * count;
  }

  public double getYSum() {
    return getYAverage() * count;
  }

  public double getXMin() {
    return count == 0 ? Double.NaN : minX;
  }

  public double getYMin() {
    return count == 0 ? Double.NaN : minY;
  }

  public double getXMax() {
    return count == 0 ? Double.NaN : maxX;
  }

  public double getYMax() {
    return count == 0 ? Double.NaN : maxY;
  }

  /** Return the number of data points. */
  public long getCount() {
    return count;
  }

  /** Merge other values into this. */
  public CovarStats combine(CovarStats that) {
    count += that.count;
    final double deltaX = that.averageX - averageX;
    final double deltaY = that.averageY - averageY;

    averageX += deltaX * that.count / count;
    squaredErrorX += that.squaredErrorX + deltaX * (that.averageX - averageX) * that.count;
    minX = Math.min(minX, that.minX);
    maxX = Math.max(maxX, that.maxX);

    jointSquaredError += that.jointSquaredError + deltaY * (that.averageX - averageX) * that.count;

    averageY += deltaX * that.count / count;
    squaredErrorY += that.squaredErrorY + deltaY * (that.averageY - averageY) * that.count;
    minY = Math.min(minY, that.minY);
    maxY = Math.max(maxY, that.maxY);

    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof CovarStats)) {
      return false;
    } else {
      CovarStats that = (CovarStats) other;
      return this.count == that.count && this.averageX == that.averageX && this.maxX == that.maxX
          && this.minX == that.minX && this.squaredErrorX == that.squaredErrorX
          && this.averageY == that.averageY && this.maxY == that.maxY && this.minY == that.minY
          && this.squaredErrorY == that.squaredErrorY
          && this.jointSquaredError == that.jointSquaredError;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, minX, maxX, averageX, squaredErrorX, minY, maxY, averageY,
        squaredErrorY, jointSquaredError);
  }

  @Override
  public String toString() {
    return "<n: " + count + ", x mean: " + averageX + ", y mean: " + averageY + ">";
  }

}
