package edu.umich.srg.marketsim.agent;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Multinomial;
import edu.umich.srg.distributions.Multinomial.IntMultinomial;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
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

/**
 * This agent is mainly for testing as it submits entirely random orders. It's also a useful
 * starting point to understand how a basic agent behaves.
 */
public class NoiseAgent implements Agent {

  private static final Distribution<OrderType> orderTypeDistribution =
      Uniform.over(OrderType.values());
  private static final IntUniform orderPriceDistribution = Uniform.closed(1000, 2000);
  private static final IntMultinomial quantityDistribution = Multinomial.withWeights(0.5, 0.3, 0.2);

  private final Sim sim;
  private final MarketView market;
  private final Random rand;
  private final Geometric arrivalDistribution;

  // Features
  private int numTransactions;

  /** Generic constructor. This can be called from java. */
  public NoiseAgent(Sim sim, Market market, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.rand = rand;

    this.numTransactions = 0;
  }

  /** The standard form construction method for creating an agent from a spimulation spec. */
  public static NoiseAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new NoiseAgent(sim, market, spec, rand);
  }

  /**
   * Lazily schedules another arrival to the market. This adds another strategy, and is called by
   * every strategy.
   */
  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  /**
   * What the agent does when it "arrives" to the market. First, withdraw all of its orders, visible
   * from marketView. Then, it submits an random order, records a feature, and schedules its next
   * arrival.
   */
  private void strategy() {
    for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
      market.withdrawOrder(order);
    }
    market.submitOrder(orderTypeDistribution.sample(rand),
        Price.of(orderPriceDistribution.sample(rand)), quantityDistribution.sample(rand) + 1);
    sim.addFeature("orders", 1);
    scheduleNextArrival();
  }

  @Override
  public void initialize() {
    scheduleNextArrival();
  }

  /**
   * This is where your agent can tell the simulator about any private values. This agent has none.
   */
  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  /**
   * This is where your agent can register any agent specific features. Here we record the number of
   * transactions. These are ignored by egta, but can be useful for your own simulations.
   */
  @Override
  public JsonObject getFeatures() {
    JsonObject features = new JsonObject();
    features.addProperty("transactions", numTransactions);
    return features;
  }

  /**
   * Since agents are unique, we use our hashcode to produce a concise id to identify this agent in
   * logs, etc.
   */
  @Override
  public String toString() {
    return "NoiseAgent " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  // Notifications
  /*
   * These are mainly at your discression to use, see documentation in Agent for when they're
   * called.
   */

  /** Want to record how many transactions we have, so we record it here. */
  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    this.numTransactions += 1;
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

