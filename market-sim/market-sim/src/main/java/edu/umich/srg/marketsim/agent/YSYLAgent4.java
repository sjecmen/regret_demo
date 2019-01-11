package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.ProfitTimeOne;
import edu.umich.srg.marketsim.Keys.ProfitTimeTwo;
import edu.umich.srg.marketsim.Keys.SpoofingTime;
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

public class YSYLAgent4 implements Agent {
  private final Sim sim;
  private final MarketView market;
  private final double fundamentalMean;
  private boolean firstSpoofing;
  private boolean quoteUpdateOn;
  private boolean stopSpoofing;
  private boolean sellUnit;
  private final TimeStamp spoofingTime;
  private final TimeStamp profitTimeOne;
  private final TimeStamp profitTimeTwo;
  private final int maxholdings = 1;



  public YSYLAgent4(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.fundamentalMean = spec.get(FundamentalMean.class);
    this.firstSpoofing = true;
    this.quoteUpdateOn = true;
    this.spoofingTime = TimeStamp.of(spec.get(SpoofingTime.class));
    this.profitTimeOne = TimeStamp.of(spec.get(ProfitTimeOne.class));
    this.profitTimeTwo = TimeStamp.of(spec.get(ProfitTimeTwo.class));
  }

  public static YSYLAgent4 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new YSYLAgent4(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
    if (Math.abs(market.getHoldings()) > maxholdings) {
      // System.err.println("position exceeds! " + sim.getCurrentTime().toString());
    }

    // before spoofing starts, buy 1 unit if there is ask less than fundamental mean
    if (sim.getCurrentTime().compareTo(profitTimeOne) <= 0) {

    } else if (sim.getCurrentTime().compareTo(profitTimeOne) > 0
        && sim.getCurrentTime().compareTo(spoofingTime) < 0) {
      if (market.getHoldings() < maxholdings) {
        if (market.getQuote().getAskPrice().isPresent()
            && market.getQuote().getAskPrice().get().doubleValue() < fundamentalMean) {
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getAskPrice().get().doubleValue()), 1);
        }
      }
    }
    // agent starts to spoof
    else {
      if (sim.getCurrentTime().compareTo(spoofingTime) == 0
          && market.getQuote().getAskPrice().isPresent()) {
        market.submitOrder(OrderType.BUY, market.getQuote().getAskPrice().get(), 1);
      }
      // start spoofing only a unit is bought before
      if (market.getHoldings() > 0) {
        quoteUpdateOn = true;
        if (firstSpoofing) {
          if (market.getQuote().getBidPrice().isPresent()) {
            market.submitOrder(OrderType.BUY,
                Price.of(market.getQuote().getBidPrice().get().doubleValue() - 1), 200);
            firstSpoofing = false;
          }
        }
        if (sim.getCurrentTime().compareTo(profitTimeTwo) >= 0) {
          if (market.getQuote().getBidPrice().isPresent()
              && market.getQuote().getBidPrice().get().doubleValue() > fundamentalMean
              && !sellUnit) {
            stopSpoofing = true;
            market.submitOrder(OrderType.SELL,
                Price.of(market.getQuote().getBidPrice().get().doubleValue()), 1);
            sellUnit = true;
            quoteUpdateOn = false;
            for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
              market.withdrawOrder(o);
            }
          }
        }
      } else {
        stopSpoofing = true;
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
    // + " ysyl");
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
    // System.out.println(stopSpoofing);
    if (sim.getCurrentTime().compareTo(spoofingTime) >= 0 && !stopSpoofing) {
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
