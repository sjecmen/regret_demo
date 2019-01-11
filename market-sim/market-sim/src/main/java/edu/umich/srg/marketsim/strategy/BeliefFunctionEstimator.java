package edu.umich.srg.marketsim.strategy;

import com.google.common.math.DoubleMath;

import Jama.Matrix;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.OrderNotification;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class BeliefFunctionEstimator {

  private final int numTran;
  private final long gracePeriod;
  private final ArrayList<OrderNotification> bidNotifications;
  private final ArrayList<OrderNotification> askNotifications;
  private final ArrayList<OrderNotification> transactionNotifications;

  public BeliefFunctionEstimator(int numTran, long gracePeriod) {
    this.numTran = numTran;
    this.gracePeriod = gracePeriod;
    this.bidNotifications = new ArrayList<>();
    this.askNotifications = new ArrayList<>();
    this.transactionNotifications = new ArrayList<>();
  }

  public static BeliefFunctionEstimator create(int numTran, long gracePeriod) {
    return new BeliefFunctionEstimator(numTran, gracePeriod);
  }

  /**
   * Add to the notification array if an order is submitted
   * 
   * @param notification
   */
  public void addOrderSubmitObservation(OrderNotification notification) {
    if (notification.getOrderType() == OrderType.BUY) {
      bidNotifications.add(notification);
    } else {
      askNotifications.add(notification);
    }
  }

  /**
   * Update the alive period and timeStamp if an order becomes inactive
   * 
   * @param notification Note: notification's timeStamp is either large than or equal to that of
   *        original order notification
   */
  public void addOrderWithdrawObservation(OrderNotification notification, boolean fromTransaction) {
    if (notification.getOrderType() == OrderType.BUY) {
      orderWithdrawUpdate(bidNotifications, notification, fromTransaction);
    } else {
      orderWithdrawUpdate(askNotifications, notification, fromTransaction);
    }
  }

  /**
   * Helper method for addOrderWithdrawObservation
   * 
   * @param notificationList
   * @param notification
   */
  private void orderWithdrawUpdate(ArrayList<OrderNotification> notificationList,
      OrderNotification notification, boolean fromTransaction) {
    // if (!fromTransaction) {
    // // find the orderNotification with the exact same price
    // for (OrderNotification o : notificationList) {
    // if (o.getPrice().equals(notification.getPrice()) && o.getPeriod() == 0) {
    // // check if instant withdraw
    // if (o.getTimeStamp().equals(notification.getTimeStamp())) {
    // o.setPeriod(-1);
    // } else {
    // o.setTimeStamp(notification.getTimeStamp());
    // o.setPeriod(notification.getTimeStamp().get() - o.getTimeStamp().get());
    // }
    // break;
    // }
    // }
    // } else {
    // for (OrderNotification o : notificationList) {
    // if (o.getTimeStamp().equals(notification.getTimeStamp())) {
    // if (notification.getOrderType() == OrderType.BUY &&
    // o.getPrice().compareTo(notification.getPrice()) >= 0) {
    // o.setPeriod(-1);
    // break;
    // }
    // if (notification.getOrderType() == OrderType.SELL &&
    // o.getPrice().compareTo(notification.getPrice()) <= 0) {
    // o.setPeriod(-1);
    // break;
    // }
    // }
    // }
    // }

    // search for the submitted order
    Collections.sort(notificationList);
    int index = Collections.binarySearch(notificationList, notification);

    // instant withdraw
    if (index >= 0) {
      if (notification.getPrice().equals(notificationList.get(index).getPrice())) {
        notificationList.get(index).setPeriod(-1);
      }
    }
    // withdrawal of a previous submitted order
    else {
      if (Math.abs(index) - 2 < 0) {
        System.err.println("The " + notification.getOrderType()
            + " order withdrawn is not in the notification array!");
      } else {
        // Example: 1 2 3 4 x 5; abs(-4-1) - 2
        OrderNotification original = notificationList.get(Math.abs(index) - 2);
        if (original.getPrice().equals(notification.getPrice())) {
          original.setPeriod(notification.getTimeStamp().get() - original.getTimeStamp().get());
          original.setTimeStamp(notification.getTimeStamp());
        } else {
          // special case of accepting a higher price buy order or a lower price sell order
          if (notification.getOrderType() == OrderType.BUY) {
            original = notificationList.get(Math.abs(index) - 1);
          }
          // prevent of counting this as a rejection
          original.setPeriod(-1);
          original.setTimeStamp(notification.getTimeStamp());
        }
      }
    }
  }

  /**
   * update order book history that leads to the most recent numTran transactions
   * 
   * @param notification
   */
  public void addTransactionObservation(OrderNotification notification) {
    // notification order type is the being accepted order type (the order with a smaller time
    // stamp, incubent order)
    OrderType type = notification.getOrderType() == OrderType.BUY ? OrderType.SELL : OrderType.BUY;
    OrderNotification o = new OrderNotification(type, notification.getPrice(),
        notification.getQuantity(), notification.getTimeStamp());
    addOrderWithdrawObservation(notification, false);
    addOrderWithdrawObservation(o, true);

    if (transactionNotifications.size() < numTran) {
      transactionNotifications.add(notification);
    }
    // update the most recent numTran, remove the oldest transaction
    else {
      Collections.sort(transactionNotifications, new timeStampComparator());
      OrderNotification removal = transactionNotifications.remove(0);

      // corner case: if two transactions happen at the same time, do not remove order history
      if (transactionNotifications.size() > 0
          && removal.getTimeStamp().equals(transactionNotifications.get(0).getTimeStamp())) {
      } else {
        // remove inactive bid/ask orders before the removed transaction
        // order with period of either -1 or a positive value is a dead one
        bidNotifications.removeIf(
            s -> (s.getPeriod() != 0 && s.getTimeStamp().compareTo(removal.getTimeStamp()) < 1));
        askNotifications.removeIf(
            s -> (s.getPeriod() != 0 && s.getTimeStamp().compareTo(removal.getTimeStamp()) < 1));
      }
      // add the new transaction
      transactionNotifications.add(notification);
    }
  }


  /*
   * The orderbook stats below only count orders in an agent's memory length
   */

  /**
   * should only apply after sorting the bidNotifications
   * 
   * @param p
   * @return volume with bid greater than or equal to p
   */
  private double bidsGreater(Price p) {
    int index = bidNotifications.size() - 1;
    double volume = 0;
    while (index != -1 && bidNotifications.get(index).getPrice().compareTo(p) >= 0) {
      volume = volume + bidNotifications.get(index).getQuantity();
      index--;
    }
    return volume;
  }

  /**
   * Note: index may be bidNotifications.size() when all the elements are smaller than p
   * 
   * @param p
   * @return index with bid greater than or equal to p
   */
  private int bidsGreaterIndex(Price p) {
    // Example: z 1 2 3 y 4 x 5 a; binarySearch output (0-1), (-3-1), (-4-1), (-5-1)
    OrderNotification o = new OrderNotification(OrderType.BUY, p, 1, TimeStamp.ZERO);
    int index = Math.abs(Collections.binarySearch(bidNotifications, o)) - 1;

    if (index == bidNotifications.size()) {
      if (p.compareTo(bidNotifications.get(index - 1).getPrice()) < 0) {
        System.err.println("wrong greater index, all smaller than case");
      }
    } else {
      if (bidNotifications.get(index).getPrice().compareTo(p) < 0) {
        System.err.println("wrong greater index");
      }
    }
    return index;
  }

  /**
   * should only apply after sorting the askNotifications
   * 
   * @param p
   * @return volume with ask less than or equal to p
   */
  private double asksLess(Price p) {
    int index = 0;
    double volume = 0;
    while (index != askNotifications.size()
        && askNotifications.get(index).getPrice().compareTo(p) <= 0) {
      volume = volume + askNotifications.get(index).getQuantity();
      index++;
    }
    return volume;
  }

  /**
   * Note: index may be -1 when all elements are greater than p
   * 
   * @param p
   * @return index with ask less than or equal to p
   */
  private int asksLessIndex(Price p) {
    // Example: e 1 a 2 3 b 4 c 5 d; (-0-1) (-1-1), (-3-1), (-4-1), (-5-1)
    OrderNotification o = new OrderNotification(OrderType.SELL, p, 1, TimeStamp.INF);
    int index = Math.abs(Collections.binarySearch(askNotifications, o)) - 2;

    if (index == -1) {
      if (p.compareTo(askNotifications.get(index + 1).getPrice()) > 0) {
        System.err.println("wrong smaller index, all greater case");
      }
    } else {
      if (askNotifications.get(index).getPrice().compareTo(p) > 0) {
        System.out.println("wrong smaller index");
      }
    }

    return Math.abs(Collections.binarySearch(askNotifications, o)) - 2;
  }

  /**
   * @param p
   * @return the volume of accepted bid orders less than or equal to p; this is also acceptedAskLess
   */
  private double acceptedBidLess(Price p) {
    int index = 0;
    double volume = 0;
    while (index != transactionNotifications.size()
        && transactionNotifications.get(index).getPrice().compareTo(p) <= 0) {
      volume = volume + transactionNotifications.get(index).getQuantity();
      index++;
    }
    return volume;
  }

  /**
   * @param p
   * @return return the volume of accepted ask orders greater than or equal to p; this is also
   *         acceptedBidGreater
   */
  private double acceptedAskGreater(Price p) {
    int index = transactionNotifications.size() - 1;
    double volume = 0;
    while (index != -1 && transactionNotifications.get(index).getPrice().compareTo(p) >= 0) {
      volume = volume + transactionNotifications.get(index).getQuantity();
      index--;
    }
    return volume;
  }

  /**
   * @return volume of rejected ask less than or equal to p (with grace period)
   * @param p
   * @param timeStamp - current time
   */
  private double rejectedAskLess(Price p, TimeStamp timeStamp) {
    int index = 0;
    double volume = 0;

    while (index != askNotifications.size()
        && askNotifications.get(index).getPrice().compareTo(p) <= 0) {
      OrderNotification notification = askNotifications.get(index);
      long period = notification.getPeriod();
      OrderNotification temp = new OrderNotification(notification.getOrderType(),
          notification.getPrice(), notification.getQuantity(), notification.getTimeStamp());
      if (period > 0 && !transactionNotifications.contains(temp)) {
        volume += (period > gracePeriod ? 1 : (double) period / gracePeriod) * temp.getQuantity();
      } else if (period == 0) {
        long alivePeriod = timeStamp.get() - notification.getTimeStamp().get();
        volume += (alivePeriod > gracePeriod ? 1 : (double) alivePeriod / gracePeriod)
            * temp.getQuantity();
      }
      index++;
    }

    return volume;
  }

  /**
   * @return volume of rejected bid greater than or equal to p
   * @param p
   * @param timeStamp
   */
  private double rejectedBidGreater(Price p, TimeStamp timeStamp) {
    int index = bidNotifications.size() - 1;
    double volume = 0;

    while (index != -1 && bidNotifications.get(index).getPrice().compareTo(p) >= 0) {
      OrderNotification notification = bidNotifications.get(index);
      long period = notification.getPeriod();
      OrderNotification temp = new OrderNotification(notification.getOrderType(),
          notification.getPrice(), notification.getQuantity(), notification.getTimeStamp());
      if (period > 0 && !transactionNotifications.contains(temp)) {
        volume += (period > gracePeriod ? 1 : (double) period / gracePeriod) * temp.getQuantity();
      } else if (period == 0) {
        long alivePeriod = timeStamp.get() - notification.getTimeStamp().get();
        volume += (alivePeriod > gracePeriod ? 1 : (double) alivePeriod / gracePeriod)
            * temp.getQuantity();
      }
      index--;
    }

    return volume;
  }

  /**
   * @param P
   * @param timeStamp - current time
   * @return the probability of a buy order being accepted
   */
  private double buyBelief(Price P, TimeStamp timeStamp) {
    double temp = acceptedBidLess(P) + asksLess(P);
    if (temp == 0) {
      return 0;
    } else {
      return temp / (temp + rejectedBidGreater(P, timeStamp));
    }
  }

  /**
   * @param P
   * @param timeStamp
   * @return the probability of a sell order being accepted
   */
  private double sellBelief(Price P, TimeStamp timeStamp) {
    double temp = acceptedAskGreater(P) + bidsGreater(P);
    if (temp == 0) {
      return 0;
    } else {
      return temp / (temp + rejectedAskLess(P, timeStamp));
    }
  }

  /*
   * Cubic Interpolation and surplus maximization
   */
  /**
   * @return coefficients of the cubic spline interpolation (Eq 7 PFDA)
   * @param p
   * @param q
   * @param yp
   * @param yq
   */
  double[] cubicSplineInterpolation(double p, double q, double yp, double yq) {
    double[][] vals = {{p * p * p, p * p, p, 1}, {q * q * q, q * q, q, 1}, {3 * p * p, 2 * p, 1, 0},
        {3 * q * q, 2 * q, 1, 0}};
    Matrix A = new Matrix(vals);
    double[] y = {yp, yq, 0, 0};
    Matrix b = new Matrix(y, 4);
    Matrix x = A.solve(b);
    return x.getColumnPackedCopy();
  }

  /**
   * @param p
   * @param q
   * @param coefficients
   * @param pVal
   * @param buyOrSell
   * @return the maximized expected surplus pair in the specific interval
   */
  Pair<Price, Double> surplusMaximization(double p, double q, double[] coefficients, double pVal,
      OrderType buyOrSell) {
    UnivariateFunction function = new MaxFunc(coefficients, pVal, buyOrSell);
    UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
    UnivariatePointValuePair pair = optimizer.optimize(new MaxEval(200), GoalType.MAXIMIZE,
        new UnivariateObjectiveFunction(function), new SearchInterval(p, q));
    long roundedPrice = DoubleMath.roundToLong(pair.getPoint(), RoundingMode.HALF_UP);
    return Pair.of(new Price((long) roundedPrice), function.value(roundedPrice));
  }

  /**
   * 
   * @param type
   * @param pVal
   * @param bBid
   * @param bAsk
   * @param timeStamp
   * @return the to submit price that maximizes the expected surplus
   */
  public Pair<Price, Double> estimate(OrderType type, double pVal, Price bBid, Price bAsk,
      TimeStamp timeStamp) {
    // if not until the numTran, act as ZIR
    if (transactionNotifications.size() < numTran) {
      if (type == OrderType.BUY) {
        return Pair.of(new Price(1), (double) 0);
      } else {
        return Pair.of(new Price(Long.MAX_VALUE), (double) 0);
      }
    }

    sortNotifications();

    // belief->interpolation->surplus maximization
    Pair<Price, Double> pair = Pair.of(new Price(0), (double) 0);
    // buyer
    if (type == OrderType.BUY) {
      // Best ask has belief 1
      double bAskBelief = 1;

      int high = Math.min(Math.max(0, bidsGreaterIndex(bAsk) - 1), bidNotifications.size() - 1);
      Price buyHigh = bidNotifications.get(high).getPrice();
      double buyHighBelief = buyBelief(buyHigh, timeStamp);

      // interpolate between buyHigh and bAsk
      if (bAsk.compareTo(buyHigh) > 0) {
        if (Double.compare(bAsk.doubleValue() - 1, buyHigh.doubleValue()) != 0) {
          if (pVal > buyHigh.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(buyHigh.doubleValue(),
                bAsk.doubleValue(), buyHighBelief, bAskBelief);
            Pair<Price, Double> temp = surplusMaximization(buyHigh.doubleValue(),
                bAsk.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        } else {
          // avoid singular matrix
          buyHigh = bAsk;
          buyHighBelief = bAskBelief;
        }
      }

      if (bAsk.equals(buyHigh))
        buyHighBelief = bAskBelief;

      int bBidIndex = bidsGreaterIndex(bBid);
      if (!bBid.equals(bidNotifications.get(bBidIndex).getPrice())) {
        System.err.println(
            bBid.doubleValue() + " " + bidNotifications.get(bBidIndex).getPrice().doubleValue());
      }
      double bBidBelief = buyBelief(bBid, timeStamp);

      int mid = DoubleMath.roundToInt((bBidIndex + high) / 2, RoundingMode.HALF_UP);
      Price buyMid = bidNotifications.get(mid).getPrice();
      double buyMidBelief = buyBelief(buyMid, timeStamp);

      // interpolate between buyMid and buyHigh
      if (buyHigh.compareTo(buyMid) > 0) {
        if (Double.compare(buyHigh.doubleValue() - 1, buyMid.doubleValue()) != 0) {
          if (pVal > buyMid.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(buyMid.doubleValue(),
                buyHigh.doubleValue(), buyMidBelief, buyHighBelief);
            Pair<Price, Double> temp = surplusMaximization(buyMid.doubleValue(),
                buyHigh.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        } else {
          // avoid singular matrix
          buyMid = buyHigh;
          buyMidBelief = buyHighBelief;
        }
      }

      if (bBidBelief == 0) {
        // last interpolation between bBid and buyMid
        if (buyMid.compareTo(bBid) > 0) {
          if (Double.compare(buyMid.doubleValue() - 1, bBid.doubleValue()) == 0) {
            bBid = new Price(bBid.longValue() - 1);
          }
          if (pVal > bBid.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(bBid.doubleValue(),
                buyMid.doubleValue(), bBidBelief, buyMidBelief);
            Pair<Price, Double> temp = surplusMaximization(bBid.doubleValue(), buyMid.doubleValue(),
                coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        }
        // System.out.println(bBid.doubleValue() + " " + buyMid.doubleValue() + " " +
        // buyHigh.doubleValue() + " " + bAsk.doubleValue() + " " + pVal);
      } else {
        // interpolate between bBid and buyMid
        if (buyMid.compareTo(bBid) > 0) {
          if (Double.compare(buyMid.doubleValue() - 1, bBid.doubleValue()) != 0) {
            if (pVal > bBid.doubleValue()) {
              double[] coefficients = cubicSplineInterpolation(bBid.doubleValue(),
                  buyMid.doubleValue(), bBidBelief, buyMidBelief);
              Pair<Price, Double> temp = surplusMaximization(bBid.doubleValue(),
                  buyMid.doubleValue(), coefficients, pVal, type);
              pair = temp.getRight() > pair.getRight() ? temp : pair;
            }
          } else {
            // avoid singular matrix
            bBid = buyMid;
            bBidBelief = buyMidBelief;
          }
        }
        // find low index
        int i = 0;
        Price buyLow = bidNotifications.get(i).getPrice();
        while (buyBelief(buyLow, timeStamp) == 0 && i != bBidIndex) {
          i++;
          buyLow = bidNotifications.get(i).getPrice();
        }
        if (i != 0) {
          buyLow = bidNotifications.get(i - 1).getPrice();
        }

        double buyLowBelief = 0;
        // last interpolation between buyLow and bBid
        if (bBid.compareTo(buyLow) > 0) {
          if (Double.compare(bBid.doubleValue() - 1, buyLow.doubleValue()) == 0) {
            buyLow = new Price(buyLow.longValue() - 1);
          }
          if (pVal > buyLow.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(buyLow.doubleValue(),
                bBid.doubleValue(), buyLowBelief, bBidBelief);
            Pair<Price, Double> temp = surplusMaximization(buyLow.doubleValue(), bBid.doubleValue(),
                coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        }
        // System.out.println(buyLow.doubleValue() + " " + bBid.doubleValue() + " " +
        // buyMid.doubleValue() + " " + buyHigh.doubleValue() + " " + bAsk.doubleValue() + " " +
        // pVal);
      }
    }
    // seller
    else {
      // Best bid has belief 1
      double bBidBelief = 1;

      int low = Math.min(Math.max(asksLessIndex(bBid) + 1, 0), askNotifications.size() - 1);
      Price sellLow = askNotifications.get(low).getPrice();
      double sellLowBelief = sellBelief(sellLow, timeStamp);

      // interpolate between bBid and sellLow
      if (sellLow.compareTo(bBid) > 0) {
        if (Double.compare(sellLow.doubleValue() - 1, bBid.doubleValue()) != 0) {
          if (pVal < sellLow.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(bBid.doubleValue(),
                sellLow.doubleValue(), bBidBelief, sellLowBelief);
            Pair<Price, Double> temp = surplusMaximization(bBid.doubleValue(),
                sellLow.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        } else {
          sellLow = bBid;
          sellLowBelief = bBidBelief;
        }
      }

      if (bBid.equals(sellLow))
        sellLowBelief = bBidBelief;

      int bAskIndex = asksLessIndex(bAsk);
      if (!bAsk.equals(askNotifications.get(bAskIndex).getPrice())) {
        System.err.println(
            bAsk.doubleValue() + " " + askNotifications.get(bAskIndex).getPrice().doubleValue());
      }
      double bAskBelief = sellBelief(bAsk, timeStamp);

      int mid = DoubleMath.roundToInt((low + bAskIndex) / 2, RoundingMode.HALF_UP);
      Price sellMid = askNotifications.get(mid).getPrice();
      double sellMidBelief = sellBelief(sellMid, timeStamp);

      // interpolate between sellLow and sellMid
      if (sellMid.compareTo(sellLow) > 0) {
        if (Double.compare(sellMid.doubleValue() - 1, sellLow.doubleValue()) != 0) {
          if (pVal < sellMid.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(sellLow.doubleValue(),
                sellMid.doubleValue(), sellLowBelief, sellMidBelief);
            Pair<Price, Double> temp = surplusMaximization(sellLow.doubleValue(),
                sellMid.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        } else {
          sellMid = sellLow;
          sellMidBelief = sellLowBelief;
        }
      }

      if (bAskBelief == 0) {
        // last interpolation between sellMid and bAsk
        if (bAsk.compareTo(sellMid) > 0) {
          if (Double.compare(bAsk.doubleValue() - 1, sellMid.doubleValue()) == 0) {
            bAsk = new Price(bAsk.longValue() + 1);
          }
          if (pVal < bAsk.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(sellMid.doubleValue(),
                bAsk.doubleValue(), sellMidBelief, bAskBelief);
            Pair<Price, Double> temp = surplusMaximization(sellMid.doubleValue(),
                bAsk.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        }
        // System.out.println(bBid.doubleValue() + " " + sellLow.doubleValue() + " " +
        // sellMid.doubleValue() + " " + bAsk.doubleValue() + " " + pVal);
      } else {
        // interpolate between sellMid and bAsk
        if (bAsk.compareTo(sellMid) > 0) {
          if (Double.compare(bAsk.doubleValue() - 1, sellMid.doubleValue()) != 0) {
            if (pVal < bAsk.doubleValue()) {
              double[] coefficients = cubicSplineInterpolation(sellMid.doubleValue(),
                  bAsk.doubleValue(), sellMidBelief, bAskBelief);
              Pair<Price, Double> temp = surplusMaximization(sellMid.doubleValue(),
                  bAsk.doubleValue(), coefficients, pVal, type);
              pair = temp.getRight() > pair.getRight() ? temp : pair;
            }
          } else {
            bAsk = sellMid;
            bAskBelief = sellMidBelief;
          }
        }
        // find high index
        int i = askNotifications.size() - 1;
        Price sellHigh = askNotifications.get(i).getPrice();
        while (i != bAskIndex && sellBelief(sellHigh, timeStamp) == 0) {
          i--;
          sellHigh = askNotifications.get(i).getPrice();
        }
        if (i != askNotifications.size() - 1) {
          sellHigh = askNotifications.get(i + 1).getPrice();
        }

        double sellHighBelief = 0;
        // last interpolation between bAsk and sellHigh
        if (sellHigh.compareTo(bAsk) > 0) {
          if (Double.compare(sellHigh.doubleValue() - 1, bAsk.doubleValue()) == 0) {
            sellHigh = new Price(sellHigh.longValue() + 1);
          }
          if (pVal < sellHigh.doubleValue()) {
            double[] coefficients = cubicSplineInterpolation(bAsk.doubleValue(),
                sellHigh.doubleValue(), bAskBelief, sellHighBelief);
            Pair<Price, Double> temp = surplusMaximization(bAsk.doubleValue(),
                sellHigh.doubleValue(), coefficients, pVal, type);
            pair = temp.getRight() > pair.getRight() ? temp : pair;
          }
        }
        // System.out.println(bBid.doubleValue() + " " + sellLow.doubleValue() + " " +
        // sellMid.doubleValue() + " " + bAsk.doubleValue() + " " + sellHigh.doubleValue() + " " +
        // pVal);
      }
    }
    return pair;
  }

  // first compare timestamp, then price
  private class timeStampComparator implements Comparator<OrderNotification> {
    public int compare(OrderNotification o1, OrderNotification o2) {
      if (!o1.getTimeStamp().equals(o2.getTimeStamp())) {
        return o1.getTimeStamp().compareTo(o2.getTimeStamp());
      } else {
        return o1.getPrice().compareTo(o2.getPrice());
      }
    }
  }

  /**
   * sort the three notification lists by price first and then timeStamp
   */
  void sortNotifications() {
    Collections.sort(bidNotifications);
    Collections.sort(askNotifications);
    Collections.sort(transactionNotifications);
  }

  public ArrayList<OrderNotification> getBidNotifications() {
    return bidNotifications;
  }

  public ArrayList<OrderNotification> getAskNotifications() {
    return askNotifications;
  }

}
