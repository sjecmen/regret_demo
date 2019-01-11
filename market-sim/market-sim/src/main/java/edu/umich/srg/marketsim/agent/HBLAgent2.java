package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.tuple.Pair;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Geometric;
import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.distributions.Uniform.IntUniform;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.NumTransactions;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.RecencyDiscount;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.strategy.BeliefFunctionEstimator2;
import edu.umich.srg.marketsim.strategy.NoisyFundamentalEstimator;
import edu.umich.srg.marketsim.strategy.SurplusThreshold;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class HBLAgent2 implements Agent {

  private static final Distribution<OrderType> orderTypeDistribution =
      Uniform.over(OrderType.values());

  protected final Sim sim;
  private final MarketView market;
  private final Fundamental trueFundamental;
  private final int maxPosition;
  private final SurplusThreshold threshold;
  private final PrivateValue privateValue;
  private final Random arrivalRand;
  private final Random shadingRand;
  private final Random typeRand;
  private final Geometric arrivalDistribution;
  private final IntUniform shadingDistribution;
  private final FundamentalView fundamental;
  private final NoisyFundamentalEstimator estimator;
  private final BeliefFunctionEstimator2 bfEstimator;
  private ArrayList<Double> beliefBias;
  private ArrayList<Double> submissionDist;

  public HBLAgent2(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.trueFundamental = fundamental;
    this.maxPosition = spec.get(MaxPosition.class);
    this.threshold = SurplusThreshold.create(spec.get(Thresh.class));
    this.privateValue = PrivateValues.gaussianPrivateValue(rand, spec.get(MaxPosition.class),
        spec.get(PrivateValueVar.class));
    this.arrivalDistribution = Geometric.withSuccessProbability(spec.get(ArrivalRate.class));
    this.shadingDistribution = Uniform.closed(250, 500);
    this.arrivalRand = new Random(rand.nextLong());
    this.shadingRand = new Random(rand.nextLong());
    this.typeRand = new Random(rand.nextLong());
    checkArgument(fundamental instanceof GaussianMeanReverting);
    this.fundamental = FundamentalView.create(sim, fundamental, TimeStamp.ZERO,
        spec.get(FundamentalObservationVariance.class), new Random(rand.nextLong()));
    this.estimator =
        NoisyFundamentalEstimator.create(spec.get(SimLength.class), spec.get(FundamentalMean.class),
            spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
            spec.get(FundamentalObservationVariance.class), spec.get(PriceVarEst.class));
    this.bfEstimator = BeliefFunctionEstimator2.create(spec.get(NumTransactions.class),
        (long) (1 / spec.get(ArrivalRate.class)), spec.get(RecencyDiscount.class));
    this.beliefBias = new ArrayList<>();
    this.submissionDist = new ArrayList<>();
  }

  public static HBLAgent2 createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new HBLAgent2(sim, market, fundamental, spec, rand);
  }

  public double getFinalFundamentalEstiamte() {
    return estimator.estimate();
  }

  public String name() {
    return "HBLAgent2";
  }

  private void scheduleNextArrival() {
    sim.scheduleIn(TimeStamp.of(1 + arrivalDistribution.sample(arrivalRand)), this::strategy);
  }

  private void strategy() {
    // withdraw previous active orders
    for (OrderRecord order : ImmutableList.copyOf(market.getActiveOrders())) {
      market.withdrawOrder(order);
    }
    estimator.addFundamentalObservation(sim.getCurrentTime(), fundamental.getFundamental());
    beliefBias.add(estimator.getCurrentEstimate()
        - trueFundamental.getValueAt(sim.getCurrentTime()).doubleValue());
    OrderType type = orderTypeDistribution.sample(typeRand);

    if (Math.abs(market.getHoldings() + type.sign()) <= maxPosition) {
      double finalEstimate = getFinalFundamentalEstiamte();
      // System.out.println(estimator.getCurrentEstimate() + " " +
      // trueFundamental.getValueAt(sim.getCurrentTime()).doubleValue() + " " +
      // sim.getCurrentTime().toString());
      double privateBenefit =
          type.sign() * privateValue.valueForExchange(market.getHoldings(), type);
      double privateVal = finalEstimate + privateBenefit;

      Price bBid;
      Price bAsk;
      Pair<Price, Double> temp;
      Price toSubmit;
      if (market.getQuote().isDefined()) {
        bBid = market.getQuote().getBidPrice().get();
        bAsk = market.getQuote().getAskPrice().get();
        temp = bfEstimator.estimate(type, privateVal, bBid, bAsk, sim.getCurrentTime());
        toSubmit = temp.getLeft();
        if (toSubmit.longValue() >= 0) {
          boolean isZIR = Long.compare(toSubmit.longValue(), (long) 1) == 0
              || Long.compare(toSubmit.longValue(), Long.MAX_VALUE) == 0;
          // if haven't reached numTran, act as ZIR
          if (isZIR) {
            double demandedSurplus = shadingDistribution.sample(shadingRand);
            toSubmit = threshold.shadePrice(type, market.getQuote(), privateVal, demandedSurplus);
            if (toSubmit.longValue() > 0) {
              market.submitOrder(type, toSubmit, 1);
              submissionDist.add(toSubmit.doubleValue() - finalEstimate);
            }
          } else {
            if (type.sign() * (privateVal - toSubmit.doubleValue()) > 0
                && Long.compare(toSubmit.longValue(), (long) 0) != 0) {
              market.submitOrder(type, toSubmit, 1);
              // double prob = type == OrderType.BUY ?
              // temp.getRight() / (privateVal - toSubmit.doubleValue()): temp.getRight() /
              // (toSubmit.doubleValue() - privateVal);
              // System.out.println(this.toString() + " " + sim.getCurrentTime().get() + " " + type
              // + " " + finalEstimate + " " + privateBenefit
              // + " " + toSubmit.doubleValue() + " " + prob);
              submissionDist.add(toSubmit.doubleValue() - finalEstimate);
            }
          }
        }
      }
      // if there is either no bBid or bAsk, act as ZIR
      else {
        double demandedSurplus = shadingDistribution.sample(shadingRand);
        toSubmit = threshold.shadePrice(type, market.getQuote(), privateVal, demandedSurplus);
        if (toSubmit.longValue() > 0) {
          market.submitOrder(type, toSubmit, 1);
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

  /*
   * Notifications
   */

  // Agent specific notifications
  @Override
  public void notifyOrderSubmitted(OrderRecord order) {
    // System.out.println(sim.getCurrentTime() + " " + order.getPrice() + " " + order.getOrderType()
    // + " HBL");
  }

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    // System.out.println(order.toString() + sim.getCurrentTime().toString());
  }

  // Market general notifications
  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {
    bfEstimator.addOrderSubmitObservation(notification);
  }

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {
    bfEstimator.addOrderWithdrawObservation(notification);
  }

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {
    bfEstimator.addTransactionObservation(notification);
  }
}
