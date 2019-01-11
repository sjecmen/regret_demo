package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalObservationVariance;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.PriceVarEst;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.strategy.NoisyFundamentalEstimator;

import java.util.Collection;
import java.util.Random;

/**
 * This agent gets noisy observations of the fundamental and uses markov assumptions of the price
 * series to get more refined estimates of the final fundamental.
 */
public class MarkovAgent extends StandardMarketAgent {
  private final FundamentalView fundamental;
  private final NoisyFundamentalEstimator estimator;

  /** Standard constructor for the Markov agent. */
  public MarkovAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, fundamental, spec, rand);
    checkArgument(fundamental instanceof GaussianMeanReverting);
    this.fundamental = FundamentalView.create(sim, fundamental, TimeStamp.ZERO,
        spec.get(FundamentalObservationVariance.class), new Random(rand.nextLong()));
    this.estimator =
        NoisyFundamentalEstimator.create(spec.get(SimLength.class), spec.get(FundamentalMean.class),
            spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
            spec.get(FundamentalObservationVariance.class), spec.get(PriceVarEst.class));
  }

  public static MarkovAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new MarkovAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  protected void strategy() {
    estimator.addFundamentalObservation(sim.getCurrentTime(), fundamental.getFundamental());
    super.strategy();
  }

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {
    estimator.addTransactionObservation(sim.getCurrentTime(), notification.getPrice(),
        notification.getQuantity());
  }

  @Override
  protected double getFinalFundamentalEstiamte() {
    return estimator.estimate();
  }

  @Override
  protected String name() {
    return "Markov";
  }

  @Override
  protected double getCurrentFundamentalEstimate() {
    return estimator.getCurrentEstimate();
  }

}
