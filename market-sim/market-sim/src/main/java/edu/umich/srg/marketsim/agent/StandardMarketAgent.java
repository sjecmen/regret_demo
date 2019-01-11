package edu.umich.srg.marketsim.agent;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public abstract class StandardMarketAgent implements Agent {

  public static enum OrderStyle {
    RANDOM, BOTH
  }

  private static final Distribution<OrderType> randomOrder = Uniform.over(OrderType.values());
  private static final Set<OrderType> allOrders = EnumSet.allOf(OrderType.class);

  protected final Sim sim;
  private final MarketView market;
  private final Fundamental trueFundamental;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Random rand;
  private final Geometric arrivalDistribution;
  private final IntUniform shadingDistribution;
  private final Supplier<Set<OrderType>> side;
  private final int ordersPerSide;
  private ArrayList<Double> beliefBias;
  private ArrayList<Double> submissionDist;

  /** Standard constructor for ZIR agent. */
  public StandardMarketAgent(Sim sim, Market market, Fundamental fundamental, Spec spec,
      Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.trueFundamental = fundamental;
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.shadingDistribution = Uniform.closed(spec.get(Rmin.class), spec.get(Rmax.class));
    switch (spec.get(Sides.class)) {
      case RANDOM:
        this.side = () -> Collections.singleton(randomOrder.sample(rand));
        break;
      case BOTH:
        this.side = () -> allOrders;
        break;
      default:
        throw new IllegalArgumentException("Sides was null");
    }
    this.ordersPerSide = spec.get(SubmitDepth.class);
    this.rand = rand;
    this.beliefBias = new ArrayList<>();
    this.submissionDist = new ArrayList<>();
  }

  protected abstract double getFinalFundamentalEstiamte();

  protected abstract double getCurrentFundamentalEstimate();

  protected abstract String name();

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(rand)), this::strategy);
  }

  protected void strategy() {
    Price prevPrice = null;
    OrderType prevType = null;
    OrderRecord prevOrder = null;
    for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
      prevPrice = order.getPrice();
      prevType = order.getOrderType();
      prevOrder = order;
    }

    Set<OrderType> sides = side.get();
    double finalEstimate = getFinalFundamentalEstiamte();

    beliefBias.add(getCurrentFundamentalEstimate()
        - trueFundamental.getValueAt(sim.getCurrentTime()).doubleValue());
    double demandedSurplus = shadingDistribution.sample(rand);

    for (OrderType type : sides) {
      for (int num = 0; num < ordersPerSide; num++) {
        if (Math.abs(market.getHoldings() + (num + 1) * type.sign()) <= maxPosition) {
          double privateBenefit = type.sign()
              * privateValue.valueForExchange(market.getHoldings() + num * type.sign(), type);
          Price toSubmit = threshold.shadePrice(type, market.getQuote(),
              finalEstimate + privateBenefit, demandedSurplus);

          if (prevType == null || prevType.sign() != type.sign()) {
            if (prevType != null) {
              market.withdrawOrder(prevOrder);
            }
            if (toSubmit.longValue() > 0) {
              market.submitOrder(type, toSubmit, 1);
              submissionDist.add(toSubmit.doubleValue() - finalEstimate);
            }
          } else {
            if (toSubmit.longValue() > 0) {
              boolean moreCompetitive = type.sign() > 0 ? toSubmit.compareTo(prevPrice) > 0
                  : toSubmit.compareTo(prevPrice) < 0;
              // if toSubmit is equal to prePrice, do nothing
              if (moreCompetitive) {
                market.submitOrder(type, toSubmit, 1);
                submissionDist.add(toSubmit.doubleValue() - finalEstimate);
                market.withdrawOrder(prevOrder);

              } else if (toSubmit.equals(prevPrice)) {
                // do nothing
              } else {
                market.withdrawOrder(prevOrder);
                market.submitOrder(type, toSubmit, 1);
                submissionDist.add(toSubmit.doubleValue() - finalEstimate);
              }

            } else {
              market.withdrawOrder(prevOrder);
            }
          }
        }
      }
    }
    scheduleNextArrival();
  }

  @Override
  public void initialize() {
    scheduleNextArrival();
  }

  @Override
  public double payoffForPosition(int position) {
    return privateValue.valueAtPosition(position);
  }

  @Override
  public ArrayList<Double> getBeliefBias() {
    return beliefBias;
  }

  @Override
  public ArrayList<Double> getSubmissionDist() {
    return submissionDist;
  }

  @Override
  public String toString() {
    return name() + " " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  // Notifications
  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    // System.out.println(this.toString() + " SUBMIT " + sim.getCurrentTime() + " " +
    // order.hashCode()
    // + " " + order.toString() + " " + market.getQuote().getBidPrice().toString() + " "
    // + market.getQuote().getAskPrice().toString() + " " + market.getQuote().getBidDepth() + " "
    // + market.getQuote().getAskDepth());
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {
    // System.out
    // .println(this.toString() + " WITHDRAW " + sim.getCurrentTime() + " " + order.hashCode()
    // + " " + order.toString() + " " + market.getQuote().getBidPrice().toString() + " "
    // + market.getQuote().getAskPrice().toString());
  }

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out
    // .println(this.toString() + " TRANSACT " + sim.getCurrentTime() + " " + order.hashCode()
    // + " " + order.toString() + " " + market.getQuote().getBidPrice().toString() + " "
    // + market.getQuote().getAskPrice().toString());
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {};

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {};

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {}
}
