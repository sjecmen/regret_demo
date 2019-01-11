package edu.umich.srg.fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

/** An order meant for use in a fourheap. */
public class Order<P extends Comparable<? super P>> implements Serializable {

  protected final OrderType type;
  protected final P price;
  protected int unmatchedQuantity; // Always positive
  protected int matchedQuantity; // Always positive
  protected final long submitTime;

  protected Order(OrderType type, P price, int initialQuantity, long submitTime) {
    checkArgument(initialQuantity > 0, "Initial quantity must be positive");
    this.price = checkNotNull(price, "Price");
    this.unmatchedQuantity = initialQuantity;
    this.matchedQuantity = 0;
    this.type = checkNotNull(type);
    this.submitTime = checkNotNull(submitTime, "Submit Time");
  }

  public OrderType getOrderType() {
    return type;
  }

  /** Get the Price. */
  public P getPrice() {
    return price;
  }

  /** Get the quantity. Always positive. */
  public int getQuantity() {
    return unmatchedQuantity + matchedQuantity;
  }

  /**
   * Get the quantity, where negative quantities indicate a sell order. Note, if an order is fully
   * transacted, this will return 0, which has no sign.
   */
  public int getTypeQuantity() {
    return type.sign() * getQuantity();
  }

  /** Returns the time this order was submitted to the fourheap, in fourheap time. */
  public long getSubmitTime() {
    return submitTime;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  /** All Orders are unique. */
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return "<" + type + " " + getQuantity() + " @ " + price + ">";
  }

  public enum OrderType {
    BUY {

      @Override
      public int sign() {
        return 1;
      }

      @Override
      public OrderType opposite() {
        return OrderType.SELL;
      }

    },
    SELL {

      @Override
      public int sign() {
        return -1;
      }

      @Override
      public OrderType opposite() {
        return OrderType.BUY;
      }

    };

    public abstract int sign();

    public abstract OrderType opposite();
  }

  private static final long serialVersionUID = 1;

}
