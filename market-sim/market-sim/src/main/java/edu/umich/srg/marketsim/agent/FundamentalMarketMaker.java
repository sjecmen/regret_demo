package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution.LongDistribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.NumRungs;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.RungSep;
import edu.umich.srg.marketsim.Keys.RungThickness;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Spread;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.strategy.MarketMakerLadder;
import edu.umich.srg.marketsim.strategy.NoisyFundamentalEstimator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class FundamentalMarketMaker implements Agent {
  // TODO Add truncation

  private final Random rand;
  private final Sim sim;
  private final MarketView market;
  private final LongDistribution arrivalDistribution;

  private final FundamentalView fundamental;
  private final NoisyFundamentalEstimator estimator;

  private final double halfSpread;
  private final int rungThickness;
  private final MarketMakerLadder strategy;

  /** Basic constructor for a simple market maker. */
  public FundamentalMarketMaker(Sim sim, Market market, Fundamental fundamental, Spec spec,
      Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));

    this.fundamental = FundamentalView.create(sim, fundamental, TimeStamp.ZERO,
        spec.get(FundamentalObservationVariance.class), rand);
    this.estimator =
        NoisyFundamentalEstimator.create(spec.get(SimLength.class), spec.get(FundamentalMean.class),
            spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
            spec.get(FundamentalObservationVariance.class), spec.get(PriceVarEst.class));

    this.halfSpread = spec.get(Spread.class) / 2;
    this.rungThickness = spec.get(RungThickness.class);
    this.strategy =
        new MarketMakerLadder(spec.get(RungSep.class), spec.get(NumRungs.class), false, false);
    this.rand = rand;
  }

  public static FundamentalMarketMaker createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new FundamentalMarketMaker(sim, market, fundamental, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  private void strategy() {
    for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
      market.withdrawOrder(o);
    }

    estimator.addFundamentalObservation(sim.getCurrentTime(), fundamental.getFundamental());
    double fundamentalPrice = estimator.estimate();

    strategy.createLadder(Price.of(fundamentalPrice - halfSpread),
        Price.of(fundamentalPrice + halfSpread)).forEach(order -> {
          if (market.getQuote().getBidPrice().isPresent() && order.getType() == OrderType.SELL
              && order.getPrice().compareTo(market.getQuote().getBidPrice().get()) < 1) {
            // truncate
          } else if (market.getQuote().getAskPrice().isPresent() && order.getType() == OrderType.BUY
              && order.getPrice().compareTo(market.getQuote().getAskPrice().get()) > -1) {
            // truncate
          } else {
            market.submitOrder(order.getType(), order.getPrice(), rungThickness);
          }
        });

    scheduleNextArrival();
  }


  @Override
  public void initialize() {
    sim.scheduleIn(TimeStamp.of(1), this::strategy);
  }

  @Override
  public double payoffForPosition(int position) {
    // No Private Value
    return 0;
  }

  @Override
  public String toString() {
    return "FMM " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    // System.out.println(this.toString() + " SUBMIT " +
    // sim.getCurrentTime() + " "
    // + order.hashCode() + " " + order.toString() + " "
    // + market.getQuote().getBidPrice().toString() + " " +
    // market.getQuote().getAskPrice().toString());
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {
    // System.out.println(this.toString() + " WITHDRAW " +
    // sim.getCurrentTime() + " "
    // + order.hashCode() + " " + order.toString() + " "
    // + market.getQuote().getBidPrice().toString() + " " +
    // market.getQuote().getAskPrice().toString());
  }

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out.println(this.toString() + " TRANSACT " +
    // sim.getCurrentTime() + " "
    // + order.hashCode() + " " + order.toString() + " "
    // + market.getQuote().getBidPrice().toString() + " " +
    // market.getQuote().getAskPrice().toString());
  }


  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {
    // TODO Auto-generated method stub

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
