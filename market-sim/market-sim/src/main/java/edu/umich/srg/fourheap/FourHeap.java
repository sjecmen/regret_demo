package edu.umich.srg.fourheap;

import static com.google.common.base.Preconditions.checkArgument;
import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import edu.umich.srg.fourheap.Order.OrderType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * This class provides an efficient order matching mechanism while also producing valid price quotes
 * in constant time. Unless noted, everything is constant time. `n` is the number of order objects,
 * not the quantity of objects i.e. adding more orders at the same price doesn't increase the
 * complexity.
 */
/*
 * TODO There's a lot of almost duplicate code about modifying matched and unmatched quantities, and
 * then inserting and removing from heaps. There's got to be a way to generalize it.
 * 
 * There are a few places where ? extends P is cast to P. For some reason eclipse (not javac)
 * complains about the comapareTo, and so this is cast to end the complaining.
 * 
 */
public class FourHeap<P extends Comparable<? super P>> implements Serializable, Iterable<Order<P>> {

  private static final long serialVersionUID = 1;
  private final Ordering<P> pord = Ordering.natural();

  protected final Queue<Order<P>> sellUnmatched;
  protected final Queue<Order<P>> sellMatched;
  protected final Queue<Order<P>> buyUnmatched;
  protected final Queue<Order<P>> buyMatched;
  protected final Ordering<Order<P>> sellUnmatchedOrdering;
  protected final Ordering<Order<P>> sellMatchedOrdering;
  protected final Ordering<Order<P>> buyMatchedOrdering;
  protected final Ordering<Order<P>> buyUnmatchedOrdering;
  private int bidDepth;
  private int askDepth;
  private long time;

  /** Create an empty fourheap. */
  public FourHeap() {
    // XXX This should just be natural().onResultOf(o -> o.price), but they need to be cast
    // serializable, which breaks the eclipse compiler
    Ordering<Order<? extends P>> priceComp = new PriceOrdering();
    Ordering<Order<? extends P>> timeComp = new TimeOrdering();

    // Note that default priority queue treats the minimum as the highest priority, so we reverse
    // price here
    sellUnmatchedOrdering = priceComp.compound(timeComp);
    sellMatchedOrdering = sellUnmatchedOrdering.reverse();
    buyUnmatchedOrdering = priceComp.reverse().compound(timeComp);
    buyMatchedOrdering = buyUnmatchedOrdering.reverse();

    this.sellUnmatched = new TreeQueue<>(sellUnmatchedOrdering); // Sout: unmatched sells, min first
    this.sellMatched = new TreeQueue<>(sellMatchedOrdering); // Sin: matched sells, max first
    this.buyUnmatched = new TreeQueue<>(buyUnmatchedOrdering); // Bout: unmatched buys, max first
    this.buyMatched = new TreeQueue<>(buyMatchedOrdering); // Bin: matched buys, min first
    this.bidDepth = 0;
    this.askDepth = 0;
    this.time = 0;
  }

  /** Inserts and returns an order into the fourheap. Complexity: O(log n). */
  public Order<P> submit(OrderType orderType, P price, int quantity) {
    checkArgument(quantity > 0, "Orders must have positive quantity");

    Order<P> order = new Order<>(orderType, price, quantity, time++);

    if (orderType == BUY) {
      bidDepth += order.unmatchedQuantity;
    } else {
      askDepth += order.unmatchedQuantity;
    }
    Queue<Order<P>> matchUnmatchedHeap;
    Queue<Order<P>> matchMatchedHeap;
    Queue<Order<P>> orderUnmatchedHeap;
    Queue<Order<P>> orderMatchedHeap;
    Ordering<Order<P>> orderMatchedOrdering;
    if (order.type == BUY) { // buy order
      orderUnmatchedHeap = buyUnmatched;
      orderMatchedHeap = buyMatched;
      orderMatchedOrdering = buyMatchedOrdering;
      matchUnmatchedHeap = sellUnmatched;
      matchMatchedHeap = sellMatched;
    } else { // sell order
      orderUnmatchedHeap = sellUnmatched;
      orderMatchedHeap = sellMatched;
      orderMatchedOrdering = sellMatchedOrdering;
      matchUnmatchedHeap = buyUnmatched;
      matchMatchedHeap = buyMatched;
    }

    // First match with unmatched orders
    while (order.unmatchedQuantity > 0 // Quantity left to match
        && !matchUnmatchedHeap.isEmpty() // Orders to match with v Can match with other order
        && ((P) matchUnmatchedHeap.peek().price).compareTo(order.price) * order.type.sign() <= 0
        && (orderMatchedHeap.isEmpty() || // Make sure it shouldn't kick out an order instead
            ((P) matchUnmatchedHeap.peek().price).compareTo(orderMatchedHeap.peek().price)
                * order.type.sign() <= 0)) {

      Order<P> match = matchUnmatchedHeap.peek();
      if (match.matchedQuantity == 0) {
        matchMatchedHeap.offer(match); // Will have nonzero matched after this
      }

      int quantityMatched = Math.min(order.unmatchedQuantity, match.unmatchedQuantity);
      order.unmatchedQuantity -= quantityMatched;
      order.matchedQuantity += quantityMatched;
      match.unmatchedQuantity -= quantityMatched;
      match.matchedQuantity += quantityMatched;

      if (match.unmatchedQuantity == 0) {
        matchUnmatchedHeap.poll(); // lost all unmatched, needed to be removed
      }
    }

    // Next displace inferior matched orders
    while (order.unmatchedQuantity > 0 // Quantity left to match
        && !orderMatchedHeap.isEmpty() // Orders to displace v Should displace order
        && orderMatchedOrdering.compare(order, orderMatchedHeap.peek()) > 0) {

      Order<P> match = orderMatchedHeap.peek();
      if (match.unmatchedQuantity == 0) {
        orderUnmatchedHeap.offer(match);
      }

      int quantityMatched = Math.min(order.unmatchedQuantity, match.matchedQuantity);
      order.unmatchedQuantity -= quantityMatched;
      order.matchedQuantity += quantityMatched;
      match.unmatchedQuantity += quantityMatched;
      match.matchedQuantity -= quantityMatched;

      if (match.matchedQuantity == 0) {
        orderMatchedHeap.poll();
      }
    }

    // Put order in necessary heaps
    if (order.unmatchedQuantity != 0) {
      orderUnmatchedHeap.offer(order);
    }
    if (order.matchedQuantity != 0) {
      orderMatchedHeap.offer(order);
    }
    return order;
  }

  /** Submit an order using negative quantity to indicate sell. */
  public Order<P> submit(P price, int quantity) {
    return submit(quantity < 0 ? SELL : BUY, price, Math.abs(quantity));
  }

  /** Withdraws a specific order. It must be in the fourheap. Complexity: O(n). */
  public void withdraw(Order<? extends P> order) {
    withdraw(order, order.getQuantity());
  }

  /**
   * Withdraws a specific quantity from an order in the fourheap. Behavior is undefined if the order
   * isn't already in the fourheap. Complexity: O(n).
   */
  public void withdraw(Order<? extends P> order, int quantity) {
    checkArgument(quantity > 0, "Quantity must be positive");
    checkArgument(quantity <= order.getQuantity(), "Can't withdraw more than in order");

    if (order.getOrderType() == BUY) {
      bidDepth -= quantity;
    } else {
      askDepth -= quantity;
    }

    Queue<Order<P>> matchUnmatchedHeap;
    Queue<Order<P>> matchMatchedHeap;
    Queue<Order<P>> orderUnmatchedHeap;
    Queue<Order<P>> orderMatchedHeap;
    if (order.type == BUY) { // buy order
      orderUnmatchedHeap = buyUnmatched;
      orderMatchedHeap = buyMatched;
      matchUnmatchedHeap = sellUnmatched;
      matchMatchedHeap = sellMatched;
    } else { // sell order
      orderUnmatchedHeap = sellUnmatched;
      orderMatchedHeap = sellMatched;
      matchUnmatchedHeap = buyUnmatched;
      matchMatchedHeap = buyMatched;
    }

    // First remove any unmatched orders (easy)
    if (order.unmatchedQuantity != 0) {
      int qremove = Math.min(quantity, order.unmatchedQuantity);
      order.unmatchedQuantity -= qremove;
      quantity -= qremove;
      if (order.unmatchedQuantity == 0) {
        orderUnmatchedHeap.remove(order);
      }
    }

    // Replace withdrawn quantity with viable orders from orderUnmatchedHeap
    while (quantity > 0 // More to remove
        && !orderUnmatchedHeap.isEmpty() // Orders to replace
        && ((P) orderUnmatchedHeap.peek().price).compareTo(matchMatchedHeap.peek().price)
            * order.type.sign() >= 0) { // Valid to match
      Order<P> match = orderUnmatchedHeap.peek();
      if (match.matchedQuantity == 0) {
        orderMatchedHeap.offer(match);
      }

      int quantityMatched = Math.min(quantity, match.unmatchedQuantity);
      order.matchedQuantity -= quantityMatched;
      match.matchedQuantity += quantityMatched;
      match.unmatchedQuantity -= quantityMatched;
      quantity -= quantityMatched;

      if (match.unmatchedQuantity == 0) {
        orderUnmatchedHeap.poll();
      }
    }

    // Remove any amount of matched orders
    while (quantity > 0) {
      Order<P> match = matchMatchedHeap.peek();
      if (match.unmatchedQuantity == 0) {
        matchUnmatchedHeap.offer(match);
      }

      int quantityMatched = Math.min(quantity, match.matchedQuantity);
      order.matchedQuantity -= quantityMatched;
      match.matchedQuantity -= quantityMatched;
      match.unmatchedQuantity += quantityMatched;
      quantity -= quantityMatched;

      if (match.matchedQuantity == 0) {
        matchMatchedHeap.poll();
      }
    }

    if (order.matchedQuantity == 0) {
      orderMatchedHeap.remove(order);
    }
  }

  public boolean contains(Order<? extends P> order) {
    return sellUnmatched.contains(order) || sellMatched.contains(order)
        || buyUnmatched.contains(order) || buyMatched.contains(order);
  }

  /**
   * Clears matching orders from the fourheap, and returns a List of MatchedOrders, which contains
   * the two matched orders, and the quantity matched by that order. Complexity: O(m) where m is the
   * number of matched orders.
   */
  public Collection<MatchedOrders<P>> clear() {
    List<Order<P>> buys = Lists.newArrayList(buyMatched);
    Collections.sort(buys, buyUnmatchedOrdering);
    List<Order<P>> sells = Lists.newArrayList(sellMatched);
    Collections.sort(sells, sellUnmatchedOrdering);

    buyMatched.clear();
    sellMatched.clear();

    Order<P> buy = null;
    Order<P> sell = null;
    Iterator<Order<P>> buyIt = buys.iterator();
    Iterator<Order<P>> sellIt = sells.iterator();

    Builder<MatchedOrders<P>> transactions = ImmutableList.builder();
    while (buyIt.hasNext() || sellIt.hasNext()) {
      if (buy == null || buy.matchedQuantity == 0) {
        buy = buyIt.next();
      }
      if (sell == null || sell.matchedQuantity == 0) {
        sell = sellIt.next();
      }

      int quantity = Math.min(buy.matchedQuantity, sell.matchedQuantity);
      buy.matchedQuantity -= quantity;
      sell.matchedQuantity -= quantity;
      transactions.add(new MatchedOrders<P>(buy, sell, quantity));
      bidDepth -= quantity;
      askDepth -= quantity;
    }
    // System.out.println(transactions.build().toString());
    return transactions.build();
  }

  /**
   * Returns the bid quote for the fourheap. A sell order with a price below this is guaranteed to
   * get matched.
   */
  public P bidQuote() {
    Order<P> sin = sellMatched.peek();
    Order<P> bout = buyUnmatched.peek();

    if (sin == null && bout == null) {
      return null;
    } else if (sin == null) {
      return bout.price;
    } else if (bout == null) {
      return sin.price;
    } else {
      return pord.max(sin.price, bout.price);
    }
  }

  /**
   * Returns the ask quote for the fourheap. A buy order with a price above this is guaranteed to
   * get matched.
   */
  public P askQuote() {
    Order<P> sout = sellUnmatched.peek();
    Order<P> bin = buyMatched.peek();

    if (bin == null && sout == null) {
      return null;
    } else if (bin == null) {
      return sout.price;
    } else if (sout == null) {
      return bin.price;
    } else {
      return pord.min(bin.price, sout.price);
    }
  }

  /** The number of orders (ignoring quantity) in the fourheap. */
  public int size() {
    return sellUnmatched.size() + sellMatched.size() + buyUnmatched.size() + buyMatched.size();
  }

  /** The number of orders weighted by quantity in the fourheap. */
  public int getNumberOfUnits() {
    return bidDepth + askDepth;
  }

  public int getBidDepth() {
    return bidDepth;
  }

  public int getAskDepth() {
    return askDepth;
  }

  public long getTime() {
    return time;
  }

  public Iterator<Order<P>> iterator() {
    return Iterators.concat(sellUnmatched.iterator(), sellMatched.iterator(),
        buyUnmatched.iterator(), buyMatched.iterator());
  }

  /** Complexity: O(n). */
  @Override
  public String toString() {
    return "<Bo: " + buyUnmatched + ", So: " + sellUnmatched + ", Bi: " + buyMatched + ", Si: "
        + sellMatched + ">";
  }

  // These had to be declared separately so they could implements serializable
  /** Sorts an Order by its price. */
  protected class PriceOrdering extends Ordering<Order<? extends P>> implements Serializable {
    private static final long serialVersionUID = 1;

    @Override
    public int compare(Order<? extends P> first, Order<? extends P> second) {
      // Eclipse can't handle the generics properly, so a manual cast is necessary
      return ((P) first.price).compareTo(second.price);
    }
  }

  /** Sorts and Order by its time. */
  protected class TimeOrdering extends Ordering<Order<? extends P>> implements Serializable {
    private static final long serialVersionUID = 1;

    @Override
    public int compare(Order<? extends P> first, Order<? extends P> second) {
      return Long.compare(first.submitTime, second.submitTime);
    }
  }

}
