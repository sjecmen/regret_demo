package edu.umich.srg.marketsim.agent;

import edu.umich.srg.egtaonline.spec.Spec;
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

public class NSNLAgent implements Agent {
  private final Sim sim;
  private final MarketView market;

  public NSNLAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
  }

  public static NSNLAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NSNLAgent(sim, market, fundamental, spec, rand);
  }

  private void strategy() {
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

  // Notifications
  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {}

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

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
