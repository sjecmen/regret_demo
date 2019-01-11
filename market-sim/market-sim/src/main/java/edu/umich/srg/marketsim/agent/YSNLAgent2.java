package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ProfitTimeOne;
import edu.umich.srg.marketsim.Keys.ProfitTimeTwo;
import edu.umich.srg.marketsim.Keys.SpoofingTimeOne;
import edu.umich.srg.marketsim.Keys.SpoofingTimeTwo;
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

public class YSNLAgent2 implements Agent {
  private final Sim sim;
  private final MarketView market;
  private boolean firstSpoofing;
  private boolean quoteUpdateOn;
  private boolean stopSpoofing;
  private final TimeStamp spoofingTimeOne;
  private final TimeStamp spoofingTimeTwo;
  private final TimeStamp profitTimeOne;
  private final TimeStamp profitTimeTwo;

  // spoofing on both buy and sell side
  public YSNLAgent2(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.firstSpoofing = true;
    this.quoteUpdateOn = true;
    this.spoofingTimeOne = TimeStamp.of(spec.get(SpoofingTimeOne.class));
    this.spoofingTimeTwo = TimeStamp.of(spec.get(SpoofingTimeTwo.class));
    this.profitTimeOne = TimeStamp.of(spec.get(ProfitTimeOne.class));
    this.profitTimeTwo = TimeStamp.of(spec.get(ProfitTimeTwo.class));
  }

  public static YSNLAgent2 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new YSNLAgent2(sim, market, fundamental, spec, rand);
  }

  private void strategy() {

    // submit spoofing buy orders
    if (sim.getCurrentTime().compareTo(spoofingTimeOne) >= 0
        && sim.getCurrentTime().compareTo(profitTimeOne) < 0) {
      quoteUpdateOn = true;
      if (firstSpoofing) {
        if (market.getQuote().getBidPrice().isPresent()) {
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), 200);
          firstSpoofing = false;
        }
      }
    }

    if (sim.getCurrentTime().compareTo(profitTimeOne) == 0) {
      for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
        market.withdrawOrder(o);
      }
      firstSpoofing = true;
    }

    // submit spoofing sell orders
    if (sim.getCurrentTime().compareTo(spoofingTimeTwo) >= 0) {
      if (sim.getCurrentTime().compareTo(profitTimeTwo) < 0) {
        quoteUpdateOn = true;
        if (firstSpoofing) {
          if (market.getQuote().getAskPrice().isPresent()) {
            market.submitOrder(OrderType.SELL,
                Price.of(market.getQuote().getAskPrice().get().doubleValue() + 1), 200);
            firstSpoofing = false;
          }
        }
      }
    }

    if (sim.getCurrentTime().compareTo(profitTimeTwo) == 0) {
      for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
        market.withdrawOrder(o);
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

  // private double getFinalFundamentalEstiamte() {
  // return estimator.estimate();
  // }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    // System.out.println(sim.getCurrentTime() + " " + order.getPrice() + " " + order.getOrderType()
    // + " ysnl2");
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    System.err.println("Orders from YSNL2 are transacted! " + order.toString() + " " + quantity
        + sim.getCurrentTime().get());
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {
    // spoofing buy orders
    if (sim.getCurrentTime().compareTo(spoofingTimeOne) >= 0
        && sim.getCurrentTime().compareTo(profitTimeOne) < 0 && !stopSpoofing) {
      if (quoteUpdateOn) {
        for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
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

    // spoofing sell orders
    if (sim.getCurrentTime().compareTo(spoofingTimeTwo) >= 0
        && sim.getCurrentTime().compareTo(profitTimeTwo) < 0 && !stopSpoofing) {
      if (quoteUpdateOn) {
        for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
          if (!order.getPrice()
              .equals(Price.of(market.getQuote().getAskPrice().get().doubleValue() + 1))) {
            quoteUpdateOn = false;
            market.withdrawOrder(order);
            quoteUpdateOn = true;
            if (market.getQuote().getAskPrice().isPresent()) {
              market.submitOrder(OrderType.SELL,
                  Price.of(market.getQuote().getAskPrice().get().doubleValue() + 1), 200);
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
