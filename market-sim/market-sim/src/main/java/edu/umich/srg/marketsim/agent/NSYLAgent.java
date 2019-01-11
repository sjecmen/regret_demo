package edu.umich.srg.marketsim.agent;

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

public class NSYLAgent implements Agent {
  private final Sim sim;
  private final MarketView market;
  private final double fundamentalMean;
  private final TimeStamp spoofingTime;
  private final TimeStamp profitTimeOne;
  private final TimeStamp profitTimeTwo;
  private final int maxholdings = 1;
  // private final FundamentalView fundamental;
  // private final NoisyFundamentalEstimator estimator;
  // private final EquilibriumEstimator eqEstimator;
  // private final long incrementalInterval;
  // private final double threshold;
  // private final int numTransactions = 5;
  // private final double rho = 0.9;

  public NSYLAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.fundamentalMean = spec.get(FundamentalMean.class);
    this.spoofingTime = TimeStamp.of(spec.get(SpoofingTime.class));
    this.profitTimeOne = TimeStamp.of(spec.get(ProfitTimeOne.class));
    this.profitTimeTwo = TimeStamp.of(spec.get(ProfitTimeTwo.class));
    // this.fundamental = FundamentalView.create(sim, fundamental, TimeStamp.ZERO,
    // spec.get(FundamentalObservationVariance.class), rand);
    // this.estimator =
    // NoisyFundamentalEstimator.create(spec.get(SimLength.class), spec.get(FundamentalMean.class),
    // spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
    // spec.get(FundamentalObservationVariance.class), spec.get(PriceVarEst.class));
    // this.eqEstimator = EquilibriumEstimator.create(numTransactions, rho);
    // this.incrementalInterval = spec.get(IncrementalInterval.class);
    // this.threshold = spec.get(Threshold.class);
  }

  public static NSYLAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NSYLAgent(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
    if (Math.abs(market.getHoldings()) > maxholdings) {
      System.err.println("position exceeds! " + sim.getCurrentTime().toString());
    }

    if (sim.getCurrentTime().compareTo(profitTimeOne) > 0
        && sim.getCurrentTime().compareTo(spoofingTime) < 0) {
      if (market.getHoldings() < maxholdings) {
        if (market.getQuote().getAskPrice().isPresent()
            && market.getQuote().getAskPrice().get().doubleValue() < fundamentalMean) {
          market.submitOrder(OrderType.BUY,
              Price.of(market.getQuote().getAskPrice().get().doubleValue()), 1);
        }
      }
    } else {
      if (market.getHoldings() > 0 && sim.getCurrentTime().compareTo(profitTimeTwo) >= 0) {
        if (market.getQuote().getBidPrice().isPresent()
            && market.getQuote().getBidPrice().get().doubleValue() > fundamentalMean) {
          market.submitOrder(OrderType.SELL,
              Price.of(market.getQuote().getBidPrice().get().doubleValue()), 1);
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

  // Notifications
  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    // System.out.println(sim.getCurrentTime() + " " + order.getPrice() + " " + order.getOrderType()
    // + " nsyl");
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out.println(sim.getCurrentTime() + " " + order.getPrice() + " " +
    // order.getOrderType());
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

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
