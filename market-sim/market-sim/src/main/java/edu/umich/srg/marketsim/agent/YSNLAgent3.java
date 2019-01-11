package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.SpoofUnits;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

public class YSNLAgent3 implements Agent {
  private final Sim sim;
  private final MarketView market;
  private boolean firstSpoofing;
  private boolean submitNewSpoof;
  private boolean fromWithdraw;
  private final TimeStamp spoofingTime = TimeStamp.of(1000);
  private final int spoofUnits;
  private ArrayList<OrderNotification> bidNotifications;
  private boolean quoteUpdateOn;
  private TimeStamp current;


  // this version of spoofing agent does not withdraw spoof orders everytime
  public YSNLAgent3(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.firstSpoofing = true;
    this.submitNewSpoof = false;
    this.fromWithdraw = false;
    this.bidNotifications = new ArrayList<>();
    this.quoteUpdateOn = true;
    this.current = TimeStamp.of(1000);
    this.spoofUnits = spec.get(SpoofUnits.class);
  }

  public static YSNLAgent3 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new YSNLAgent3(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
    // put spoofing orders from spoofingTime to the end of the simulation
    if (sim.getCurrentTime().compareTo(spoofingTime) >= 0) {
      quoteUpdateOn = true;
      if (firstSpoofing) {
        if (market.getQuote().getBidPrice().isPresent()) {
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), spoofUnits);
          firstSpoofing = false;
        }
      }

      if (market.getQuote().getBidPrice().isPresent()) {
        // System.out.println(market.getQuote().getBidPrice().get().doubleValue() + " " +
        // sim.getCurrentTime().toString());
        // System.out.println(market.getActiveOrders().toString());
        for (OrderRecord orderRecord : market.getActiveOrders()) {
          if (orderRecord.getPrice().equals(market.getQuote().getBidPrice().get())) {
            System.err
                .println("spoofing orders become the best bid " + sim.getCurrentTime().toString());
          }
        }
      }
    }
    scheduleNextArrival();
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1), this::strategy);
  }

  @Override
  public void initialize() {
    sim.scheduleIn(TimeStamp.of(1), this::strategy);
  }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    submitNewSpoof = true;
    firstSpoofing = false;
    // if (market.getQuote().getBidPrice().isPresent()) {
    // System.out.println(market.getQuote().getBidPrice().get().doubleValue());
    // }
    // System.out.println(order.toString() + " is submitted at " + sim.getCurrentTime().toString());
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {
    if (submitNewSpoof) {
      submitNewSpoof = false;
    }
    // if (market.getQuote().getBidPrice().isPresent()) {
    // System.out.println(market.getQuote().getBidPrice().get().doubleValue());
    // }
    // System.out.println(order.toString() + " is withdrawn at " + sim.getCurrentTime().toString());
  }

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    System.err.println("Orders from YSNL are transacted! " + order.toString() + " " + quantity + " "
        + sim.getCurrentTime().toString());
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {
    if (sim.getCurrentTime().compareTo(spoofingTime) >= 0) {

      if (sim.getCurrentTime().compareTo(current) > 0) {
        submitNewSpoof = false;
        current = sim.getCurrentTime();
      }

      if (quoteUpdateOn) {
        // no bids at all
        if (bidNotifications.isEmpty()) {
          quoteUpdateOn = false;
          for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
            market.withdrawOrder(order);
          }
          quoteUpdateOn = true;
        } else {
          // get maximum of spoofing buy orders
          Price maximumSpoof = Price.of(0);
          for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
            if (order.getPrice().compareTo(maximumSpoof) > 0) {
              maximumSpoof = order.getPrice();
            }
          }

          if (market.getQuote().getBidPrice().get()
              .compareTo(Price.of(maximumSpoof.doubleValue() + 1)) > 0) {
            if (!submitNewSpoof) {
              market.submitOrder(OrderType.BUY,
                  Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), spoofUnits);
            }
          } else if (market.getQuote().getBidPrice().get().equals(maximumSpoof)) {
            // get maximum price of nonspoofing buy orders
            OrderNotification maximumUnit = Collections.max(bidNotifications);
            boolean submitNew = true;
            quoteUpdateOn = false;
            for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
              if (order.getPrice().compareTo(maximumUnit.getPrice()) >= 0) {
                market.withdrawOrder(order);
              }
              if (order.getPrice().equals(Price.of(maximumUnit.getPrice().doubleValue() - 1))) {
                submitNew = false;
              }
            }
            quoteUpdateOn = true;

            if (submitNew && !submitNewSpoof) {
              market.submitOrder(OrderType.BUY, Price.of(maximumUnit.getPrice().doubleValue() - 1),
                  spoofUnits);
            }
          }
        }
      }
    }
  }

  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {
    if (notification.getOrderType() == OrderType.BUY && notification.getQuantity() == 1) {
      bidNotifications.add(notification);
      // System.out.println("in submit " + notification.toString());
    }
  }

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {
    if (notification.getOrderType() == OrderType.BUY && notification.getQuantity() == 1) {
      Collections.sort(bidNotifications);
      int index = Collections.binarySearch(bidNotifications, notification);
      if (index >= 0) {
        if (notification.getPrice().equals(bidNotifications.get(index).getPrice())) {
          bidNotifications.remove(index);
        }
      } else {
        if (Math.abs(index) - 2 < 0) {
          System.err.println("A: The " + notification.getOrderType()
              + " order withdrawn is not in the notification array!");
        } else {
          // Example: 1 2 3 4 x 5; abs(-4-1) - 2
          OrderNotification original = bidNotifications.get(Math.abs(index) - 2);
          if (original.getPrice().equals(notification.getPrice())) {
            bidNotifications.remove(Math.abs(index) - 2);
          } else {
            System.err.println("B: The " + notification.getOrderType()
                + " order withdrawn is not in the notification array!");
          }
        }
      }
      // System.out.println("in withdraw " + notification.toString());
    }
  }

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {
    if (notification.getOrderType() == OrderType.BUY) {
      Collections.sort(bidNotifications);
      int index = Collections.binarySearch(bidNotifications, notification);
      if (index >= 0) {
        if (notification.getPrice().equals(bidNotifications.get(index).getPrice())) {
          bidNotifications.remove(index);
        }
      } else {
        if (Math.abs(index) - 2 < 0) {
          System.err.println("A: The " + notification.getOrderType()
              + " order withdrawn is not in the notification array!");
        } else {
          // Example: 1 2 3 4 x 5; abs(-4-1) - 2
          OrderNotification original = bidNotifications.get(Math.abs(index) - 2);
          if (original.getPrice().equals(notification.getPrice())) {
            bidNotifications.remove(Math.abs(index) - 2);
          } else {
            System.err.println("B: The " + notification.getOrderType()
                + " order withdrawn is not in the notification array!");
          }
        }
      }
    } else {
      bidNotifications.remove(bidNotifications.size() - 1);
    }
    // System.out.println("in transaction " + notification.toString());
  }

  @Override
  public ArrayList<Double> getBeliefBias() {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<Double> getSubmissionDist() {
    return new ArrayList<>();
  }

}
