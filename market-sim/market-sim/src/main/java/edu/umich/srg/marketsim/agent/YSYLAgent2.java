package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
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

public class YSYLAgent2 implements Agent {
  private final Sim sim;
  private final MarketView market;
  private final double fundamentalMean;
  private boolean firstSpoofing;
  private boolean quoteUpdateOn;
  private boolean stopSpoofing;
  private final TimeStamp spoofingTimeOne;
  private final TimeStamp spoofingTimeTwo;
  private final TimeStamp profitTimeOne;
  private final TimeStamp profitTimeTwo;
  private final int maxholdings = 1;


  // spoof buy -> true sell -> spoof sell -> true buy
  public YSYLAgent2(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.fundamentalMean = spec.get(FundamentalMean.class);
    this.firstSpoofing = true;
    this.quoteUpdateOn = true;
    this.spoofingTimeOne = TimeStamp.of(spec.get(SpoofingTimeOne.class));
    this.spoofingTimeTwo = TimeStamp.of(spec.get(SpoofingTimeTwo.class));
    this.profitTimeOne = TimeStamp.of(spec.get(ProfitTimeOne.class));
    this.profitTimeTwo = TimeStamp.of(spec.get(ProfitTimeTwo.class));
  }

  public static YSYLAgent2 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new YSYLAgent2(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
    if (Math.abs(market.getHoldings()) > maxholdings) {
      System.err.println("position exceeds! " + sim.getCurrentTime().toString());
    }
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
    }
    // submit true sell order
    if (sim.getCurrentTime().compareTo(profitTimeOne) >= 0
        && sim.getCurrentTime().compareTo(spoofingTimeTwo) < 0
        && Math.abs(market.getHoldings()) < maxholdings) {
      if (market.getQuote().getBidPrice().isPresent()
          && market.getQuote().getBidPrice().get().doubleValue() > fundamentalMean) {
        stopSpoofing = true;
        market.submitOrder(OrderType.SELL,
            Price.of(market.getQuote().getBidPrice().get().doubleValue()), 1);
        quoteUpdateOn = false;
        for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
          market.withdrawOrder(o);
        }
      }
    }

    // submit spoofing sell orders
    if (sim.getCurrentTime().compareTo(spoofingTimeTwo) >= 0) {
      if (market.getHoldings() < 0) {
        stopSpoofing = false;
        if (sim.getCurrentTime().compareTo(spoofingTimeTwo) == 0) {
          firstSpoofing = true;
        }
      }
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
      if (sim.getCurrentTime().compareTo(profitTimeTwo) == 0) {
        for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
          market.withdrawOrder(o);
        }
      }
      // submit true buy order
      if (sim.getCurrentTime().compareTo(profitTimeTwo) >= 0 && !(market.getHoldings() == 0)) {
        if (market.getQuote().getAskPrice().isPresent()
            && market.getQuote().getAskPrice().get().doubleValue() < fundamentalMean) {
          stopSpoofing = true;
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getAskPrice().get().doubleValue()), 1);
          quoteUpdateOn = false;
          for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
            market.withdrawOrder(o);
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
    // + " ysyl2");
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out.println(sim.getCurrentTime() + " " + order.getPrice() + " " +
    // order.getOrderType());
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
  public void notifyTransaction(MarketView market, OrderNotification notification) {
    // eqEstimator.addTransactionObservation(notification);
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
