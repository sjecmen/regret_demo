package edu.umich.srg.marketsim.agent;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.FundamentalView;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.strategy.GaussianFundamentalEstimator;

import java.util.Collection;
import java.util.Random;

public class ZirAgent extends StandardMarketAgent {

  private final FundamentalView fundamental;
  private final GaussianFundamentalEstimator estimator;
  // private FourHeap<Price> orderbook;

  /** Standard constructor for ZIR agent. */
  public ZirAgent(Sim sim, Market market, Fundamental fundamental, Spec spec, Random rand) {
    super(sim, market, fundamental, spec, rand);
    checkArgument(fundamental instanceof GaussianMeanReverting);

    this.fundamental = FundamentalView.create(sim, fundamental);
    this.estimator = GaussianFundamentalEstimator.create(spec.get(SimLength.class),
        spec.get(FundamentalMean.class), spec.get(FundamentalMeanReversion.class));
  }

  public static ZirAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new ZirAgent(sim, market, fundamental, spec, rand);
  }

  @Override
  protected double getFinalFundamentalEstiamte() {
    return estimator.estimate(sim.getCurrentTime(), fundamental.getFundamental());
  }

  @Override
  protected String name() {
    return "ZIR";
  }

  @Override
  protected double getCurrentFundamentalEstimate() {
    return fundamental.getFundamental().doubleValue();
  }

}
