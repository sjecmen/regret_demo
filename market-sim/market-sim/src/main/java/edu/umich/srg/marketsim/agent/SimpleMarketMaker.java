package edu.umich.srg.marketsim.agent;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution.LongDistribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.NumRungs;
import edu.umich.srg.marketsim.Keys.RungSep;
import edu.umich.srg.marketsim.Keys.RungThickness;
import edu.umich.srg.marketsim.Keys.TickImprovement;
import edu.umich.srg.marketsim.Keys.TickOutside;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.market.Quote;
import edu.umich.srg.marketsim.strategy.MarketMakerLadder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class SimpleMarketMaker implements Agent {

  private Optional<Price> lastBid;
  private Optional<Price> lastAsk;

  private final Random rand;
  private final Sim sim;
  private final MarketView market;
  private final LongDistribution arrivalDistribution;
  private final int rungThickness;
  private final MarketMakerLadder strategy;

  /** Basic constructor for a simple market maker. */
  public SimpleMarketMaker(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.rungThickness = spec.get(RungThickness.class);
    this.strategy = new MarketMakerLadder(spec.get(RungSep.class), spec.get(NumRungs.class),
        spec.get(TickImprovement.class), spec.get(TickOutside.class));
    this.lastBid = this.lastAsk = Optional.absent();
    this.rand = rand;
  }

  public static SimpleMarketMaker createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new SimpleMarketMaker(sim, market, spec, rand);
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  private void strategy() {
    updateQuote();
    for (OrderRecord o : ImmutableList.copyOf(market.getActiveOrders())) {
      market.withdrawOrder(o);
    }
    updateQuote();

    if (lastBid.isPresent() && lastAsk.isPresent()) {
      strategy.createLadder(lastBid.get(), lastAsk.get()).forEach(order -> {
        market.submitOrder(order.getType(), order.getPrice(), rungThickness);
      });
    }

    scheduleNextArrival();
  }

  private void updateQuote() {
    Quote quote = market.getQuote();
    this.lastBid = quote.getBidPrice().or(lastBid);
    this.lastAsk = quote.getAskPrice().or(lastAsk);
  }

  @Override
  public void initialize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForPosition(int position) {
    // No Private Value
    return 0;
  }

  @Override
  public String toString() {
    return "SMM " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
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
  public void notifyTransaction(MarketView market, OrderNotification notification) {
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
