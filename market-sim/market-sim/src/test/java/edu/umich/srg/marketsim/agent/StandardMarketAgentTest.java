package edu.umich.srg.marketsim.agent;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.Sides;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.SubmitDepth;
import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.agent.StandardMarketAgent.OrderStyle;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.Quote;

import java.util.Random;

public class StandardMarketAgentTest {

  private static final Random rand = new Random();
  private static final double fundamentalMean = 1e9;
  private static final Spec base =
      Spec.builder().putAll(Keys.DEFAULT_KEYS).put(ArrivalRate.class, 0.5).put(Rmin.class, 100)
          .put(Rmax.class, 500).put(MaxPosition.class, 5).put(PrivateValueVar.class, 1e3)
          .put(SimLength.class, 10L).put(FundamentalMeanReversion.class, 0d).build();

  /** Test that both sides submits an order to both sides. */
  @Test
  public void bothSidesTest() {
    Fundamental fundamental = ConstantFundamental.create(fundamentalMean);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    Spec spec = Spec.fromDefaultPairs(base, Sides.class, OrderStyle.BOTH);

    StandardMarketAgent agent = new StandardMarketAgent(sim, cda, spec, rand) {
      @Override
      protected double getFinalFundamentalEstiamte() {
        return fundamentalMean;
      }

      @Override
      protected String name() {
        return "SMA";
      }
    };
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert buy and sell afterwards
    quote = view.getQuote();
    assertEquals(1, quote.getAskDepth());
    assertEquals(1, quote.getBidDepth());
  }

  /** Test that setting submit depth works. */
  @Test
  public void multiOrderTest() {
    Fundamental fundamental = ConstantFundamental.create(fundamentalMean);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    Spec spec = Spec.fromDefaultPairs(base, SubmitDepth.class, 2);

    StandardMarketAgent agent = new StandardMarketAgent(sim, cda, spec, rand) {
      @Override
      protected double getFinalFundamentalEstiamte() {
        return fundamentalMean;
      }

      @Override
      protected String name() {
        return "SMA";
      }
    };
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert 2 buys or 2 sells
    quote = view.getQuote();
    assertEquals(ImmutableSet.of(0, 2), ImmutableSet.of(quote.getAskDepth(), quote.getBidDepth()));
  }

  /** Test that both sides and submit depth submits multiple orders to both sides. */
  @Test
  public void bothSidesAndSubmitDepthTest() {
    Fundamental fundamental = ConstantFundamental.create(fundamentalMean);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    Spec spec = Spec.fromDefaultPairs(base, Sides.class, OrderStyle.BOTH, SubmitDepth.class, 3);

    StandardMarketAgent agent = new StandardMarketAgent(sim, cda, spec, rand) {
      @Override
      protected double getFinalFundamentalEstiamte() {
        return fundamentalMean;
      }

      @Override
      protected String name() {
        return "SMA";
      }
    };
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert buy and sell afterwards
    quote = view.getQuote();
    assertEquals(3, quote.getAskDepth());
    assertEquals(3, quote.getBidDepth());
  }

  /** Test that setting submit depth works, but is clipped if max position would be exceeded. */
  @Test
  public void clippedMultiOrderTest() {
    Fundamental fundamental = ConstantFundamental.create(fundamentalMean);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    Spec spec = Spec.fromDefaultPairs(base, SubmitDepth.class, 3, MaxPosition.class, 2);

    StandardMarketAgent agent = new StandardMarketAgent(sim, cda, spec, rand) {
      @Override
      protected double getFinalFundamentalEstiamte() {
        return fundamentalMean;
      }

      @Override
      protected String name() {
        return "SMA";
      }
    };
    MarketView view = cda.getView(agent);
    Quote quote;

    // Assert quote empty before
    quote = view.getQuote();
    assertEquals(0, quote.getAskDepth());
    assertEquals(0, quote.getBidDepth());

    agent.strategy();

    // Assert 2 buys or 2 sells
    quote = view.getQuote();
    assertEquals(ImmutableSet.of(0, 2), ImmutableSet.of(quote.getAskDepth(), quote.getBidDepth()));
  }

}
