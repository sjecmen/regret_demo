package edu.umich.srg.marketsim.privatevalue;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.testing.Asserts;
import edu.umich.srg.testing.TestInts;

import java.util.Random;

@RunWith(Theories.class)
public class ListPrivateValueTest {

  private static final Random rand = new Random();
  private static final DoubleDistribution[] distributions =
      {Gaussian.withMeanVariance(0, 100), Uniform.closedOpen(0d, 1000d)};

  @Theory
  public void decreasingMarginalValueTest(@TestInts({1, 10, 100}) int maxPos,
      @TestInts({0, 1}) int distIndex) {
    DoubleDistribution dist = distributions[distIndex];
    PrivateValue pv = new ListPrivateValue(dist, maxPos, rand);
    for (int pos = -maxPos - 1; pos < maxPos; ++pos) {
      double high = pv.valueForExchange(pos, BUY);
      double low = pv.valueForExchange(pos + 1, BUY);
      Asserts.assertTrue(high >= low, "%f !>= %f", high, low);
    }
    for (int pos = maxPos + 1; pos > -maxPos; --pos) {
      double high = pv.valueForExchange(pos, SELL);
      double low = pv.valueForExchange(pos - 1, SELL);
      Asserts.assertTrue(high >= low, "%f !>= %f", high, low);
    }
  }

  @Test
  public void emptyTest() {
    PrivateValue pv = new ListPrivateValue(new double[0]);

    // Assert all gains are 0
    assertEquals(0, pv.valueForExchange(-1, BUY), 0);
    assertEquals(0, pv.valueForExchange(0, BUY), 0);
    assertEquals(0, pv.valueForExchange(1, BUY), 0);

    assertEquals(0, pv.valueForExchange(-1, SELL), 0);
    assertEquals(0, pv.valueForExchange(0, SELL), 0);
    assertEquals(0, pv.valueForExchange(1, SELL), 0);

    // Assert all positions are 0
    assertEquals(0, pv.valueAtPosition(-10), 0);
    assertEquals(0, pv.valueAtPosition(-1), 0);
    assertEquals(0, pv.valueAtPosition(0), 0);
    assertEquals(0, pv.valueAtPosition(1), 0);
    assertEquals(0, pv.valueAtPosition(10), 0);
  }

  @Test
  public void simpleBuyerTest() {
    PrivateValue pv = new ListPrivateValue(new double[] {2, 1});

    // Assert on gains
    assertEquals(2, pv.valueForExchange(-2, BUY), 0);
    assertEquals(2, pv.valueForExchange(-1, BUY), 0);
    assertEquals(1, pv.valueForExchange(0, BUY), 0);
    assertEquals(0, pv.valueForExchange(1, BUY), 0);
    assertEquals(0, pv.valueForExchange(2, BUY), 0);

    assertEquals(-2, pv.valueForExchange(-2, SELL), 0);
    assertEquals(-2, pv.valueForExchange(-1, SELL), 0);
    assertEquals(-2, pv.valueForExchange(0, SELL), 0);
    assertEquals(-1, pv.valueForExchange(1, SELL), 0);
    assertEquals(0, pv.valueForExchange(2, SELL), 0);

    // Assert on total value
    assertEquals(-4, pv.valueAtPosition(-2), 0);
    assertEquals(-2, pv.valueAtPosition(-1), 0);
    assertEquals(0, pv.valueAtPosition(0), 0);
    assertEquals(1, pv.valueAtPosition(1), 0);
    assertEquals(1, pv.valueAtPosition(2), 0);
  }

  @Test
  public void simpleSellerTest() {
    PrivateValue pv = new ListPrivateValue(new double[] {-1, -2});

    // Assert on gains
    assertEquals(0, pv.valueForExchange(-2, BUY), 0);
    assertEquals(-1, pv.valueForExchange(-1, BUY), 0);
    assertEquals(-2, pv.valueForExchange(0, BUY), 0);
    assertEquals(-2, pv.valueForExchange(1, BUY), 0);
    assertEquals(-2, pv.valueForExchange(2, BUY), 0);

    assertEquals(0, pv.valueForExchange(-2, SELL), 0);
    assertEquals(0, pv.valueForExchange(-1, SELL), 0);
    assertEquals(1, pv.valueForExchange(0, SELL), 0);
    assertEquals(2, pv.valueForExchange(1, SELL), 0);
    assertEquals(2, pv.valueForExchange(2, SELL), 0);

    // Assert on total value
    assertEquals(1, pv.valueAtPosition(-2), 0);
    assertEquals(1, pv.valueAtPosition(-1), 0);
    assertEquals(0, pv.valueAtPosition(0), 0);
    assertEquals(-2, pv.valueAtPosition(1), 0);
    assertEquals(-4, pv.valueAtPosition(2), 0);
  }

  @Test
  public void simpleNoTradeTest() {
    PrivateValue pv = new ListPrivateValue(new double[] {1, -2});

    // Assert on gains
    assertEquals(1, pv.valueForExchange(-2, BUY), 0);
    assertEquals(1, pv.valueForExchange(-1, BUY), 0);
    assertEquals(-2, pv.valueForExchange(0, BUY), 0);
    assertEquals(-2, pv.valueForExchange(1, BUY), 0);
    assertEquals(-2, pv.valueForExchange(2, BUY), 0);

    assertEquals(-1, pv.valueForExchange(-2, SELL), 0);
    assertEquals(-1, pv.valueForExchange(-1, SELL), 0);
    assertEquals(-1, pv.valueForExchange(0, SELL), 0);
    assertEquals(2, pv.valueForExchange(1, SELL), 0);
    assertEquals(2, pv.valueForExchange(2, SELL), 0);

    // Assert on total value
    assertEquals(-2, pv.valueAtPosition(-2), 0);
    assertEquals(-1, pv.valueAtPosition(-1), 0);
    assertEquals(0, pv.valueAtPosition(0), 0);
    assertEquals(-2, pv.valueAtPosition(1), 0);
    assertEquals(-4, pv.valueAtPosition(2), 0);
  }

  @Test
  public void ComplexTest() {
    PrivateValue pv = new ListPrivateValue(new double[] {3, 2, -1, -4, -5}, 3);

    // Assert on gains
    assertEquals(3, pv.valueForExchange(-4, BUY), 0);
    assertEquals(3, pv.valueForExchange(-3, BUY), 0);
    assertEquals(2, pv.valueForExchange(-2, BUY), 0);
    assertEquals(-1, pv.valueForExchange(-1, BUY), 0);
    assertEquals(-4, pv.valueForExchange(0, BUY), 0);
    assertEquals(-5, pv.valueForExchange(1, BUY), 0);
    assertEquals(-5, pv.valueForExchange(2, BUY), 0);

    assertEquals(-3, pv.valueForExchange(-3, SELL), 0);
    assertEquals(-3, pv.valueForExchange(-2, SELL), 0);
    assertEquals(-2, pv.valueForExchange(-1, SELL), 0);
    assertEquals(1, pv.valueForExchange(0, SELL), 0);
    assertEquals(4, pv.valueForExchange(1, SELL), 0);
    assertEquals(5, pv.valueForExchange(2, SELL), 0);
    assertEquals(5, pv.valueForExchange(3, SELL), 0);

    // Assert on total value
    assertEquals(-7, pv.valueAtPosition(-4), 0);
    assertEquals(-4, pv.valueAtPosition(-3), 0);
    assertEquals(-1, pv.valueAtPosition(-2), 0);
    assertEquals(1, pv.valueAtPosition(-1), 0);
    assertEquals(0, pv.valueAtPosition(0), 0);
    assertEquals(-4, pv.valueAtPosition(1), 0);
    assertEquals(-9, pv.valueAtPosition(2), 0);
    assertEquals(-14, pv.valueAtPosition(3), 0);

  }

}
