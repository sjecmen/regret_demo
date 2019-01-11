package edu.umich.srg.marketsim.strategy;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static edu.umich.srg.testing.Asserts.assertSetEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import edu.umich.srg.marketsim.Price;

import java.util.Set;
import java.util.stream.Collectors;

public class MarketMakerLadderTest {

  @Test
  public void basicTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, false, false);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(100), Price.of(150)).collect(Collectors.toSet());
    Set<OrderDesc> expected = createLadder(3, 160, 155, 150, 100, 95, 90);
    assertSetEquals(created, expected);
  }

  @Test
  public void overlapTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, false, false);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(100), Price.of(100)).collect(Collectors.toSet());
    Set<OrderDesc> expected = createLadder(2, 110, 105, 95, 90);
    assertSetEquals(created, expected);
  }

  @Test
  public void crossedTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, false, false);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(110), Price.of(100)).collect(Collectors.toSet());
    Set<OrderDesc> expected = ImmutableSet.of();
    assertSetEquals(created, expected);
  }

  @Test
  public void tickImprovementTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, true, false);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(100), Price.of(150)).collect(Collectors.toSet());
    Set<OrderDesc> expected = createLadder(3, 159, 154, 149, 101, 96, 91);
    assertSetEquals(created, expected);
  }

  @Test
  public void tickOutsideTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, true, true);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(100), Price.of(150)).collect(Collectors.toSet());
    Set<OrderDesc> expected = createLadder(3, 161, 156, 151, 99, 94, 89);
    assertSetEquals(created, expected);
  }

  @Test
  public void tickImprovementOverlapTest() {
    MarketMakerLadder strat = new MarketMakerLadder(5, 3, true, false);
    Set<OrderDesc> created =
        strat.createLadder(Price.of(100), Price.of(102)).collect(Collectors.toSet());
    Set<OrderDesc> expected = createLadder(2, 111, 106, 96, 91);
    assertSetEquals(created, expected);
  }

  @Test
  public void IllegalArgumentsTest() {
    try {
      new MarketMakerLadder(0, 3, true, false);
      fail("Allowed ladder with 0 step size");
    } catch (IllegalArgumentException ex) {
      // expected
    }

    try {
      new MarketMakerLadder(5, 0, true, false);
      fail("Allowed ladder with 0 rungs");
    } catch (IllegalArgumentException ex) {
      // expected
    }
  }

  /** Creates an order ladder to assert against, first `numSells` or `prices` are sells */
  private static Set<OrderDesc> createLadder(int numSells, long... prices) {
    ImmutableSet.Builder<OrderDesc> ladder = ImmutableSet.builder();
    for (int i = 0; i < numSells; ++i) {
      ladder.add(OrderDesc.of(SELL, Price.of(prices[i])));
    }
    for (int i = numSells; i < prices.length; ++i) {
      ladder.add(OrderDesc.of(BUY, Price.of(prices[i])));
    }
    return ladder.build();
  }

}
