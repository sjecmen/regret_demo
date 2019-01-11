package edu.umich.srg.marketsim.agent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.MaxPosition;
import edu.umich.srg.marketsim.Keys.PrivateValueVar;
import edu.umich.srg.marketsim.Keys.Rmax;
import edu.umich.srg.marketsim.Keys.Rmin;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;

import java.util.Map;
import java.util.Random;

public class ZirTest {

  private static final Random rand = new Random();

  @Test
  public void integrationTest() {
    int numAgents = 10;
    long simLength = 10;

    Fundamental fundamental = ConstantFundamental.create(1e9);
    MarketSimulator sim = MarketSimulator.create(fundamental, rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    Spec spec =
        Spec.builder().putAll(Keys.DEFAULT_KEYS).put(ArrivalRate.class, 0.5).put(Rmin.class, 100)
            .put(Rmax.class, 500).put(MaxPosition.class, 5).put(PrivateValueVar.class, 1e3)
            .put(SimLength.class, simLength).put(FundamentalMeanReversion.class, 0d).build();
    for (int i = 0; i < numAgents; ++i) {
      sim.addAgent(new ZirAgent(sim, cda, fundamental, spec, rand));
    }
    sim.initialize();
    sim.executeUntil(TimeStamp.of(simLength));

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(numAgents, payoffs.size());
  }

}
