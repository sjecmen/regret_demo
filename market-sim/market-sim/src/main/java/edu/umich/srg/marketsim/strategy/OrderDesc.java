package edu.umich.srg.marketsim.strategy;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;

import java.util.Objects;

/** The description of an order that an agent should submit. */
public class OrderDesc {

  private final OrderType type;
  private final Price price;

  private OrderDesc(OrderType type, Price price) {
    this.type = type;
    this.price = price;
  }

  public static OrderDesc of(OrderType type, Price price) {
    return new OrderDesc(type, price);
  }

  public OrderType getType() {
    return type;
  }

  public Price getPrice() {
    return price;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof OrderDesc)) {
      return false;
    } else {
      OrderDesc that = (OrderDesc) other;
      return Objects.equals(this.price, that.price) && Objects.equals(this.price, that.price);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, price);
  }

  @Override
  public String toString() {
    return type + " @ " + price;
  }
}
