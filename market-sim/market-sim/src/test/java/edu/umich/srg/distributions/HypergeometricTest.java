package edu.umich.srg.distributions;

import static edu.umich.srg.testing.Asserts.assertChiSquared;
import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/*
 * These tests will all fail with some probability so they aren't run by default. It is believed
 * that this code is accurate. If the hypergeometric code is changed, these should be rerun.
 */
@Ignore
public class HypergeometricTest {

  private static final Random rand = new Random();
  private static final double eps = 1e-14;
  private static final int n = 1000000;

  @Test
  public void test_14_5_3() {
    double[] expected =
        {0.230769230769231, 0.494505494505494, 0.247252747252747, 0.0274725274725275};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(14, 5, 3);
    int[] observed = new int[4];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

  @Test
  public void test_14_5_8() {
    double[] expected = {0.002997002997003, 0.05994005994006, 0.27972027972028, 0.41958041958042,
        0.20979020979021, 0.027972027972028};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(14, 5, 8);
    int[] observed = new int[6];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

  @Test
  public void test_14_8_5() {
    double[] expected = {0.002997002997003, 0.05994005994006, 0.27972027972028, 0.41958041958042,
        0.20979020979021, 0.027972027972028};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(14, 8, 5);
    int[] observed = new int[6];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

  @Test
  public void test_14_8_9() {
    double[] expected = {0, 0, 0, 0.027972027972028, 0.20979020979021, 0.41958041958042,
        0.27972027972028, 0.0599400599400599, 0.002997002997003};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(14, 8, 9);
    int[] observed = new int[9];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

  @Test
  public void test_15_8_5() {
    double[] expected = {0.0069930069930068, 0.0932400932400932, 0.326340326340326,
        0.391608391608392, 0.163170163170163, 0.018648018648019};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(15, 8, 5);
    int[] observed = new int[6];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

  @Test
  public void test_15_8_9() {
    double[] expected = {0, 0, 0.0055944055944056, 0.0783216783216783, 0.293706293706294,
        0.391608391608392, 0.195804195804196, 0.0335664335664336, 0.0013986013986014};
    assertEquals(1, Arrays.stream(expected).sum(), eps);

    Hypergeometric dist = Hypergeometric.with(15, 8, 9);
    int[] observed = new int[9];
    for (int i = 0; i < n; ++i)
      observed[dist.sample(rand)] += 1;
    assertChiSquared(expected, observed);
  }

}
