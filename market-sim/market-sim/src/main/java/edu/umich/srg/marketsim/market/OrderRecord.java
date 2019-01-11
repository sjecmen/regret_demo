package edu.umich.srg.marketsim.market;


import static edu.umich.srg.fourheap.Order.OrderType.BUY;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.Market.OrderInfo;

public class OrderRecord implements OrderInfo, Comparable<OrderRecord> {

  final MarketView submittedMarket;
  final OrderType buyOrSell;
  final Price price;
  int quantity;

  public OrderRecord(MarketView submittedMarket, OrderType buyOrSell, Price price, int quantity) {
    this.submittedMarket = submittedMarket;
    this.buyOrSell = buyOrSell;
    this.price = price;
    this.quantity = quantity;
  }

  public OrderType getOrderType() {
    return buyOrSell;
  }

  public Price getPrice() {
    return price;
  }

  public int getQuantity() {
    return quantity;
  }

  @Override
  public String toString() {
    return (buyOrSell == BUY ? "Buy" : "Sell") + ' ' + quantity + " @ " + price;
  }

  @Override
  public int compareTo(OrderRecord o) {
    Price oPrice = o.getPrice();
    return this.price.compareTo(oPrice);
  }

}
