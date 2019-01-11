package edu.umich.srg.fourheap;

import java.io.Serializable;

public class MatchedOrders<P extends Comparable<? super P>> implements Serializable {

  private static final long serialVersionUID = -6073835626927361670L;

  protected final Order<P> buy;
  protected final Order<P> sell;
  protected final int quantity;

  protected MatchedOrders(Order<P> buy, Order<P> sell, int quantity) {
    this.buy = buy;
    this.sell = sell;
    this.quantity = quantity;
  }

  public Order<P> getBuy() {
    return buy;
  }

  public Order<P> getSell() {
    return sell;
  }

  public int getQuantity() {
    return quantity;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return "<buy=" + buy + ", sell=" + sell + ", quantity=" + quantity + ">";
  }

}
