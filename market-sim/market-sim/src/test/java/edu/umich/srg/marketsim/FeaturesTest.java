package edu.umich.srg.marketsim;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static edu.umich.srg.testing.Asserts.assertCompletesIn;
import static edu.umich.srg.testing.Asserts.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockProb;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.Thresh;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ZirAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;
import edu.umich.srg.testing.TestBools;
import edu.umich.srg.testing.TestInts;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RunWith(Theories.class)
public class FeaturesTest {

  private static final Random rand = new Random();
  private static final double tol = 1e-6;
  private static final Spec spec = Spec.builder().putAll(Keys.DEFAULT_KEYS) //
      .put(ArrivalRate.class, 0.5) //
      .put(MaxPosition.class, 10) //
      .put(Thresh.class, 1.0) //
      .put(PrivateValueVar.class, 1000.0) //
      .put(Rmin.class, 0) //
      .put(Rmax.class, 100) //
      .put(SimLength.class, 20L) //
      .put(FundamentalMean.class, (double) Integer.MAX_VALUE / 2) //
      .put(FundamentalMeanReversion.class, 0.1) //
      .put(FundamentalShockVar.class, 100.0) //
      .build();

  @Rule
  public final RepeatRule repeatRule = new RepeatRule();

  @Repeat(10)
  @Theory
  public void simpleRandomTest(@TestInts({10}) int numAgents,
      @TestBools({false, true}) boolean intermediate) {
    Fundamental fundamental = GaussianMeanReverting.create(rand, spec.get(FundamentalMean.class),
        spec.get(FundamentalMeanReversion.class), spec.get(FundamentalShockVar.class),
        spec.get(FundamentalShockProb.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(new ZirAgent(sim, cda, fundamental, spec, rand));
    }
    if (intermediate) {
      sim.addAgent(new NoiseAgent(sim, cda, spec, rand));
    }
    sim.initialize();
    sim.executeUntil(TimeStamp.of(spec.get(SimLength.class)));
    JsonObject features = sim.computeFeatures();

    double surplus = features.get("total_surplus").getAsDouble();
    double maxSurplus = features.get("max_surplus").getAsDouble();
    double imSurplusLoss = features.get("im_surplus_loss").getAsDouble();
    double emSurplusLoss = features.get("em_surplus_loss").getAsDouble();
    double maxSurplusSubLim = features.get("max_surplus_sublim").getAsDouble();
    double imSurplusLossSubLim = features.get("im_surplus_loss_sublim").getAsDouble();
    double emSurplusLossSubLim = features.get("em_surplus_loss_sublim").getAsDouble();

    // All of these should already be tested by asserts, but are included for completeness / in case
    // someone doesn't turn on asserts
    // Assert that surplus is nonnegative
    assertTrue(maxSurplus >= 0, "max surplus negative %f", maxSurplus);
    assertTrue(maxSurplusSubLim >= 0, "submission constrained max surplus negative %f",
        maxSurplusSubLim);
    // Assert that max surpluses are greater than realized surplus
    assertTrue(surplus <= maxSurplus);
    assertTrue(surplus <= maxSurplusSubLim);
    // Assert that max surplus - losses = surplus
    assertEquals(surplus, maxSurplus - imSurplusLoss - emSurplusLoss, tol);
    assertEquals(surplus, maxSurplusSubLim - imSurplusLossSubLim - emSurplusLossSubLim, tol);
    // Assert that restricting by submissions only lowers surplus
    assertTrue(maxSurplusSubLim <= maxSurplus);
  }

  /** Test that intermediary improves surplus. */
  @Theory
  public void intermediateBenefitTest(@TestInts({2}) int numAgents) {
    // Private Value of agent that wants to buy
    PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {10, 3});

    // First create simulation with only "background" agents
    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(MockAgent.builder().privateValue(pv).build());
    }

    // Verify that surplus is 0 because they won't trade
    sim.initialize();
    double maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
    assertEquals(0, maxSurplus, tol);

    // Now add an intermediary (no private value)
    sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(MockAgent.builder().privateValue(pv).build());
    }
    sim.addAgent(MockAgent.create());

    // Assert that each agent trades with intermediary for 3 surplus
    sim.initialize();
    maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
    assertEquals(3 * numAgents, maxSurplus, tol);
  }

  /**
   * Test that in situation where agents could infinitely trade, max surplus still terminates due to
   * diminishing returns in private values.
   */
  @Test
  public void infiniteTradeTest() throws ExecutionException, InterruptedException {
    assertCompletesIn(() -> {
      MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
      // Buyer
      sim.addAgent(MockAgent.builder()
          .privateValue(PrivateValues.fromMarginalBuys(new double[] {1, 1})).build());
      // Seller
      sim.addAgent(MockAgent.builder()
          .privateValue(PrivateValues.fromMarginalBuys(new double[] {-1, -1})).build());

      // Verify correct surplus is calculated
      sim.initialize();
      double maxSurplus = sim.computeFeatures().get("max_surplus").getAsDouble();
      assertEquals(2, maxSurplus, tol);
    }, 5, TimeUnit.SECONDS);
  }

  /** Test that submissions is accurately counted. */
  @Test
  public void submissionsTest() {
    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));

    // Buy 2 @ 200
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);

      @Override
      public void initialize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(BUY, Price.of(200), 2);
        });
      }
    });

    // Sell 1 @ 100
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);

      @Override
      public void initialize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(SELL, Price.of(100), 1);
        });
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(2));

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(2, payoffs.size());
    assertEquals(0, payoffs.values().stream().mapToDouble(AgentInfo::getProfit).sum(), tol);
    Set<Integer> holdings = payoffs.values().stream().mapToInt(AgentInfo::getHoldings).boxed()
        .collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(-1, 1), holdings);
    Set<Integer> submissions = payoffs.values().stream().mapToInt(AgentInfo::getSubmissions).boxed()
        .collect(Collectors.toSet());
    assertEquals(ImmutableSet.of(1, 2), submissions);
  }

  @Test
  public void noTradeTest() {
    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));

    // Buyer willing to buy at 5
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);
      final PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {100, 5});

      @Override
      public void initialize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(BUY, Price.of(200), 2);
        });
      }

      @Override
      public double payoffForPosition(int position) {
        return pv.valueAtPosition(position);
      }
    });

    // Seller willing to sell at 10
    sim.addAgent(new MockAgent() {
      final MarketView view = cda.getView(this, TimeStamp.ZERO);
      final PrivateValue pv = PrivateValues.fromMarginalBuys(new double[] {10, -100});

      @Override
      public void initialize() {
        sim.scheduleIn(TimeStamp.of(1), () -> {
          view.submitOrder(SELL, Price.of(100), 1);
        });
      }

      @Override
      public double payoffForPosition(int position) {
        return pv.valueAtPosition(position);
      }
    });

    sim.initialize();
    sim.executeUntil(TimeStamp.of(2));

    JsonObject features = sim.computeFeatures();
    double surplus = features.get("total_surplus").getAsDouble();
    double maxSurplus = features.get("max_surplus").getAsDouble();
    double imSurplusLoss = features.get("im_surplus_loss").getAsDouble();
    double emSurplusLoss = features.get("em_surplus_loss").getAsDouble();

    // Verify that max surplus is 0
    assertEquals(0, maxSurplus, tol);
    assertEquals(surplus, maxSurplus - imSurplusLoss - emSurplusLoss, tol);
  }

}
