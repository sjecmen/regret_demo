package edu.umich.srg.marketsim.market;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.Market.OrderInfo;

public class OrderNotification implements OrderInfo, Comparable<OrderNotification> {

  final OrderType buyOrSell;
  final Price price;
  private TimeStamp timeStamp; // when the notification is generated, will be updated to the time of
                               // withdraw or transaction
  private int quantity;
  private long period; // alive period (-1 is submission and withdraw are at the same time, 0 if
                       // still alive, positive if already dead)

  public OrderNotification(OrderType buyOrSell, Price price, int quantity, TimeStamp timeStamp) {
    this.buyOrSell = buyOrSell;
    this.price = price;
    this.quantity = quantity;
    this.timeStamp = timeStamp;
    this.period = (long) 0;
  }

  public TimeStamp getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(TimeStamp t) {
    this.timeStamp = t;
  }

  public void setPeriod(long period) {
    this.period = period;
  }

  public long getPeriod() {
    return this.period;
  }

  @Override
  public Price getPrice() {
    return price;
  }

  @Override
  public int getQuantity() {
    return quantity;
  }

  @Override
  public OrderType getOrderType() {
    return buyOrSell;
  }

  @Override
  public String toString() {
    return timeStamp.toString() + (buyOrSell == BUY ? " Buy" : " Sell") + ' ' + quantity + " @ "
        + price + " period " + period;
  }

  @Override
  // first compare price, then compare time stamp; no comparison on order type or quantity
  public int compareTo(OrderNotification o) {
    Price oPrice = o.getPrice();
    if (!this.price.equals(oPrice)) {
      return this.price.compareTo(oPrice);
    } else {
      return this.timeStamp.compareTo(o.timeStamp);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof OrderNotification)) {
      return false;
    } else {
      OrderNotification that = (OrderNotification) other;
      return this.timeStamp.equals(that.timeStamp) && this.buyOrSell.equals(that.buyOrSell)
          && this.price.equals(that.price) && this.quantity == that.quantity;
    }
  }

}
