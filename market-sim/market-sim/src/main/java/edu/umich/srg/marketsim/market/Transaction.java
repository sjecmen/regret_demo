package edu.umich.srg.marketsim.market;

import edu.umich.srg.marketsim.Price;

import java.io.Serializable;

/**
 * Base class for Transactions. Contains information on buyer/seller, market, quantity, price, and
 * time.
 * 
 * @author ewah
 */
public class Transaction implements Serializable {

  // Transaction Info
  private final int quantity;
  private final Price price;

  protected Transaction(int quantity, Price price) {
    this.quantity = quantity;
    this.price = price;
  }

  public final int getQuantity() {
    return quantity;
  }

  public final Price getPrice() {
    return price;
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
    return quantity + " @ " + price;
  }

  private static final long serialVersionUID = 8420827805792281642L;

}
