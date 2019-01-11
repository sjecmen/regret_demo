package edu.umich.srg.marketsim.strategy;

import edu.umich.srg.marketsim.market.OrderNotification;

import java.util.ArrayList;

public class EquilibriumEstimator {

  private final int numTran;
  private final double rho;
  private final ArrayList<OrderNotification> transactionNotifications;

  public EquilibriumEstimator(int numTran, double rho) {
    this.numTran = numTran;
    this.rho = rho;
    this.transactionNotifications = new ArrayList<>();
  }

  public static EquilibriumEstimator create(int numTran, double rho) {
    return new EquilibriumEstimator(numTran, rho);
  }

  /**
   * Update the array to keep the most recent numTran (ordered by time)
   * 
   * @param notification
   */
  public void addTransactionObservation(OrderNotification notification) {
    if (transactionNotifications.size() < numTran) {
      transactionNotifications.add(notification);
    } else {
      transactionNotifications.remove(0);
      transactionNotifications.add(notification);
    }
  }

  /**
   * @return the weighted moving average of the most recent transactions
   */
  public double estimateEq() {
    if (transactionNotifications.size() < numTran) {
      return Double.NaN;
    } else {
      double sumPrice = 0;
      double weight = 1;
      double sumWeight = 0;
      for (int i = numTran - 1; i >= 0; i--) {
        sumPrice += weight * transactionNotifications.get(i).getPrice().doubleValue();
        sumWeight += weight;
        weight = weight * rho;
      }
      return sumPrice / sumWeight;
    }
  }

  public void clear() {
    transactionNotifications.clear();
  }
}
