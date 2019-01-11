package edu.umich.srg.testing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

public abstract class Asserts {

  private static final Map<Double, Integer> confidenceMap =
      ImmutableMap.of(0.90, 0, 0.95, 1, 0.975, 2, 0.99, 3, 0.999, 4);
  private static final double[][] chiSquared =
      {{2.706, 3.841, 5.024, 6.635, 10.828}, {4.605, 5.991, 7.378, 9.210, 13.816},
          {6.251, 7.815, 9.348, 11.345, 16.266}, {7.779, 9.488, 11.143, 13.277, 18.467},
          {9.236, 11.070, 12.833, 15.086, 20.515}, {10.645, 12.592, 14.449, 16.812, 22.458},
          {12.017, 14.067, 16.013, 18.475, 24.322}, {13.362, 15.507, 17.535, 20.090, 26.125},
          {14.684, 16.919, 19.023, 21.666, 27.877}, {15.987, 18.307, 20.483, 23.209, 29.588},
          {17.275, 19.675, 21.920, 24.725, 31.264}, {18.549, 21.026, 23.337, 26.217, 32.910},
          {19.812, 22.362, 24.736, 27.688, 34.528}, {21.064, 23.685, 26.119, 29.141, 36.123},
          {22.307, 24.996, 27.488, 30.578, 37.697}, {23.542, 26.296, 28.845, 32.000, 39.252},
          {24.769, 27.587, 30.191, 33.409, 40.790}, {25.989, 28.869, 31.526, 34.805, 42.312},
          {27.204, 30.144, 32.852, 36.191, 43.820}, {28.412, 31.410, 34.170, 37.566, 45.315}};


  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual) {
    assertSetEquals(expected, actual, "Sets not equal");
  }

  public static <T> void assertSetEquals(Set<? extends T> expected, Set<? extends T> actual,
      String message) {
    if (checkNotNull(expected).equals(checkNotNull(actual)))
      return;

    SetView<? extends T> extra = Sets.difference(actual, expected),
        missing = Sets.difference(expected, actual);

    if (extra.isEmpty()) {
      throw new AssertionError(String.format("%s - missing: %s", message, missing));
    } else if (missing.isEmpty()) {
      throw new AssertionError(String.format("%s - extra: %s", message, extra));
    } else {
      throw new AssertionError(
          String.format("%s - missing: %s - extra: %s", message, missing, extra));
    }
  }

  public static void assertChiSquared(double[] expected, int[] observed) {
    assertChiSquared(expected, observed, 0.95);
  }

  public static void assertChiSquared(double[] expected, int[] observed, double confidence) {
    int total = Arrays.stream(observed).sum();
    double testStatistic =
        IntStream.range(0, expected.length).mapToDouble(i -> (observed[i] - expected[i] * total)
            * (observed[i] / (double) total - expected[i]) / expected[i]).sum();
    double criticalValue = chiSquared[expected.length - 2][confidenceMap.get(confidence)];
    assertTrue(testStatistic < criticalValue,
        "Assuming expected, observed would be seen less than %f%% of the time (saw\n%s\ninstead of\n%s)",
        1 - confidence,
        Arrays.toString(Arrays.stream(observed).mapToDouble(i -> i / (double) total).toArray()),
        Arrays.toString(expected));
  }

  public static void assertTrue(boolean assertion, String formatString, Object... objects) {
    if (!assertion) {
      throw new AssertionError(String.format(formatString, objects));
    }
  }

  public static void assertCompletesIn(Runnable task, long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Future<?> running = executor.submit(task);
    try {
      running.get(timeout, unit);
    } catch (TimeoutException e) {
      throw new AssertionError(
          "Task did not complete in " + timeout + " " + unit.toString().toLowerCase());
    } catch (ExecutionException e) {
      throw e;
    } finally {
      executor.shutdownNow();
    }
  }

}

