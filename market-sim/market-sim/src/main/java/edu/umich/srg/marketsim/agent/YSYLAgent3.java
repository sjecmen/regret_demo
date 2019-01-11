package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
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
import java.util.Random;

public class YSYLAgent3 implements Agent {
  private final Sim sim;
  private final MarketView market;
  private boolean firstSpoofing;
  private boolean quoteUpdateOn;
  private final TimeStamp spoofingTime = TimeStamp.of(1000);

  // Beside buy side spoofing, submit a buy order at 1000;
  public YSYLAgent3(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.firstSpoofing = true;
    this.quoteUpdateOn = true;
  }

  public static YSYLAgent3 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new YSYLAgent3(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
    // put spoofing orders from spoofingTime to the end of the simulation
    if (sim.getCurrentTime().compareTo(spoofingTime) >= 0) {
      if (sim.getCurrentTime().compareTo(spoofingTime) == 0
          && market.getQuote().getAskPrice().isPresent()) {
        market.submitOrder(OrderType.BUY, market.getQuote().getAskPrice().get(), 1);
      }
      quoteUpdateOn = true;
      if (firstSpoofing) {
        if (market.getQuote().getBidPrice().isPresent()) {
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), 200);
          firstSpoofing = false;
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
    // System.out.println(order.toString() + " is submitted at " + sim.getCurrentTime().toString());
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {
    // System.out.println(order.toString() + " is withdrawn at " + sim.getCurrentTime().toString());
  }

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out.println("Orders from YSYL3 are transacted! " + order.toString() + " " + quantity);
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {
    if (sim.getCurrentTime().compareTo(spoofingTime) >= 0) {
      if (quoteUpdateOn) {
        for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
          // if there is a change on the best bid, withdraw the original spoofing orders and put new
          // ones
          if (!order.getPrice()
              .equals(Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1))) {
            quoteUpdateOn = false;
            market.withdrawOrder(order);
            quoteUpdateOn = true;
            if (market.getQuote().getBidPrice().isPresent()) {
              market.submitOrder(OrderType.BUY,
                  Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), 200);
            } else {
              firstSpoofing = true;
            }
          }
        }
      }
    }
  }

  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {}

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {}

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {}

  @Override
  public ArrayList<Double> getBeliefBias() {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<Double> getSubmissionDist() {
    return new ArrayList<>();
  }

}
