package edu.umich.srg.marketsim.strategy;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.OrderNotification;

import java.util.Collections;

public class BeliefFunctionTest {

  private static final double eps = 0.001;
  private static final int NumTransactions = 2;
  private static final long gracePeriod = 200;

  @Test
  public void BeliefTest() {

    BeliefFunctionEstimator estimator =
        BeliefFunctionEstimator.create(NumTransactions, gracePeriod);
    OrderNotification s1 =
        new OrderNotification(OrderType.SELL, new Price(100045), 1, TimeStamp.of(4));
    estimator.addOrderSubmitObservation(s1);
    OrderNotification s2 =
        new OrderNotification(OrderType.SELL, new Price(100017), 1, TimeStamp.of(25));
    estimator.addOrderSubmitObservation(s2);
    OrderNotification s3 =
        new OrderNotification(OrderType.SELL, new Price(100753), 1, TimeStamp.of(40));
    estimator.addOrderSubmitObservation(s3);
    OrderNotification b1 =
        new OrderNotification(OrderType.BUY, new Price(99231), 1, TimeStamp.of(71));
    estimator.addOrderSubmitObservation(b1);
    OrderNotification b2 =
        new OrderNotification(OrderType.BUY, new Price(100017), 1, TimeStamp.of(86));
    estimator.addOrderSubmitObservation(b2);
    estimator.addTransactionObservation(b2);
    OrderNotification s4 =
        new OrderNotification(OrderType.SELL, new Price(100015), 1, TimeStamp.of(93));
    estimator.addOrderSubmitObservation(s4);
    OrderNotification s5 =
        new OrderNotification(OrderType.SELL, new Price(101161), 1, TimeStamp.of(96));
    estimator.addOrderSubmitObservation(s5);
    OrderNotification s6 =
        new OrderNotification(OrderType.SELL, new Price(100868), 1, TimeStamp.of(117));
    estimator.addOrderSubmitObservation(s6);
    OrderNotification b3 =
        new OrderNotification(OrderType.BUY, new Price(100015), 1, TimeStamp.of(121));
    estimator.addOrderSubmitObservation(b3);
    estimator.addTransactionObservation(b3);

    estimator.sortNotifications();

    // assertEquals((double) 0, estimator.sellBelief(new Price(100050)), eps);
    // assertEquals((double) 1, estimator.buyBelief(new Price(100015)), eps);
    // assertEquals(new Price(99907), estimator.estimate(OrderType.BUY, 100800, new Price(99231),
    // new Price(100045)));
  }

  @Test
  public void withdrawNotificationTest() {
    BeliefFunctionEstimator estimator = BeliefFunctionEstimator.create(2, 200, 10);
    OrderNotification s1 = new OrderNotification(OrderType.SELL, new Price(3), 1, TimeStamp.of(4));
    estimator.addOrderSubmitObservation(s1);
    OrderNotification s2 = new OrderNotification(OrderType.SELL, new Price(9), 1, TimeStamp.of(25));
    estimator.addOrderSubmitObservation(s2);
    OrderNotification s3 = new OrderNotification(OrderType.SELL, new Price(6), 1, TimeStamp.of(40));
    estimator.addOrderSubmitObservation(s3);
    OrderNotification s4 =
        new OrderNotification(OrderType.SELL, new Price(12), 1, TimeStamp.of(93));
    estimator.addOrderSubmitObservation(s4);
    OrderNotification s5 = new OrderNotification(OrderType.SELL, new Price(7), 1, TimeStamp.of(96));
    estimator.addOrderSubmitObservation(s5);
    OrderNotification s6 =
        new OrderNotification(OrderType.SELL, new Price(12), 1, TimeStamp.of(117));
    OrderNotification s7 = new OrderNotification(OrderType.SELL, new Price(3), 1, TimeStamp.of(0));
    OrderNotification s8 = new OrderNotification(OrderType.BUY, new Price(2), 1, TimeStamp.of(156));
    estimator.sortNotifications();
    long a = 3568;
    long b = 3568 * 3;

    assertEquals(4,
        Math.abs(Collections.binarySearch(estimator.getAskSubmitNotifications(), s6)) - 2, eps);
    assertEquals(true, Collections.binarySearch(estimator.getAskSubmitNotifications(), s7) == -1);
    assertEquals(-1,
        Math.abs(Collections.binarySearch(estimator.getAskSubmitNotifications(), s8)) - 2, eps);
    assertEquals((double) 1 / 3, (double) a / b, eps);
  }

  @Test
  public void surplusMaxTest() {
    BeliefFunctionEstimator estimator = BeliefFunctionEstimator.create(2, 200, 10);
    Pair<Price, Double> pair = estimator.surplusMaximization(100273, 100486,
        estimator.cubicSplineInterpolation(100273, 100486, 1, 4 / 4.08), 100633.9389088088,
        OrderType.SELL);
    // assertEquals((double) 100485, pair.getLeft().doubleValue(), eps);
    assertEquals((double) -145.03815, pair.getRight(), eps);
    Pair<Price, Double> pair2 = estimator.surplusMaximization(100486, 100515,
        estimator.cubicSplineInterpolation(100486, 100515, 4 / 4.08, 2 / 2.08), 100633.9389088088,
        OrderType.SELL);
    assertEquals((double) 100514, pair2.getLeft().doubleValue(), eps);
    assertEquals((double) -114.364, pair2.getRight(), eps);
    Pair<Price, Double> pair3 = estimator.surplusMaximization(100515, 100738,
        estimator.cubicSplineInterpolation(100515, 100738, 2 / 2.08, 0), 100633.9389088088,
        OrderType.SELL);
    assertEquals((double) 100671, pair3.getLeft().doubleValue(), eps);
    assertEquals((double) 7.719, pair3.getRight(), eps);
    Pair<Price, Double> pair4 = estimator.surplusMaximization(99721, 100134,
        estimator.cubicSplineInterpolation(99721, 100134, 2 / 2.51, 1), 100633.9389088088,
        OrderType.BUY);
    assertEquals((double) 99721, pair4.getLeft().doubleValue(), eps);
    assertEquals((double) 727.441, pair4.getRight(), eps);
    Pair<Price, Double> pair5 = estimator.surplusMaximization(99712, 99721,
        estimator.cubicSplineInterpolation(99712, 99721, 0, 2 / 2.51), 100633.9389088088,
        OrderType.BUY);
    assertEquals((double) 99720, pair5.getLeft().doubleValue(), eps);
    assertEquals((double) 727.816, pair5.getRight(), eps);
    Pair<Price, Double> other = estimator.surplusMaximization(9.963, 10.026,
        estimator.cubicSplineInterpolation(9.963, 10.026, 0, 1), 9.988, OrderType.BUY);
    assertEquals((double) 9, other.getLeft().doubleValue(), eps);

    double[] coefficients = estimator.cubicSplineInterpolation(100083, 100085, 4 / 8.08, 4 / 9.08);
    double[] expected = {0, 0, 0, 0};
    // assertEquals(expected, coefficients);
  }

}
