package edu.umich.srg.marketsim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.FundamentalMarketMaker;
import edu.umich.srg.marketsim.agent.HBLAgent;
import edu.umich.srg.marketsim.agent.HBLAgent2;
import edu.umich.srg.marketsim.agent.MarkovAgent;
import edu.umich.srg.marketsim.agent.NSNLAgent;
import edu.umich.srg.marketsim.agent.NSYLAgent;
import edu.umich.srg.marketsim.agent.NSYLAgent2;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.agent.ShockAgent;
import edu.umich.srg.marketsim.agent.SimpleMarketMaker;
import edu.umich.srg.marketsim.agent.SimpleTrendFollower;
import edu.umich.srg.marketsim.agent.YSNLAgent;
import edu.umich.srg.marketsim.agent.YSNLAgent2;
import edu.umich.srg.marketsim.agent.YSNLAgent3;
import edu.umich.srg.marketsim.agent.YSYLAgent;
import edu.umich.srg.marketsim.agent.YSYLAgent2;
import edu.umich.srg.marketsim.agent.YSYLAgent3;
import edu.umich.srg.marketsim.agent.YSYLAgent4;
import edu.umich.srg.marketsim.agent.ZirAgent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CallMarket;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

final class EntityBuilder {

  static AgentCreator getAgentCreator(String name) {
    return checkNotNull(agentNameMap.get(name),
        "\"%s\" is not a defined agent name in EntityBuilder", name);
  }

  static MarketCreator getMarketCreator(String name) {
    return checkNotNull(marketNameMap.get(name),
        "\"%s\" is not a defined market name in EntityBuilder", name);
  }

  private static final Map<String, AgentCreator> agentNameMap =
      ImmutableMap.<String, AgentCreator>builder() //
          .put("noise", NoiseAgent::createFromSpec) // Noise agentas for testings
          .put("zir", ZirAgent::createFromSpec) // Standard ZI agents with re-entry
          .put("markov", MarkovAgent::createFromSpec) // ZI agents with markov learning
          .put("smm", SimpleMarketMaker::createFromSpec) // Simple Market Maker
          .put("fmm", FundamentalMarketMaker::createFromSpec) // Fundamental Market Maker
          .put("shock", ShockAgent::createFromSpec) // Shock agent that buys a lot at a random time
          .put("trend", SimpleTrendFollower::createFromSpec) // Simple trend follower
          .put("nsnl", NSNLAgent::createFromSpec) // arrive at every time stamp but doing nothing
          .put("nsyl", NSYLAgent::createFromSpec) // just provide liquidity
          .put("ysnl", YSNLAgent::createFromSpec) // constantly put spoofing orders after a specific
                                                  // time stamp
          .put("ysnl2", YSNLAgent2::createFromSpec).put("ysnl3", YSNLAgent3::createFromSpec) // do
                                                                                             // not
                                                                                             // spoof
                                                                                             // everytime
          .put("ysyl", YSYLAgent::createFromSpec) // try to
                                                  // profit
                                                  // from
                                                  // spoofing
                                                  // (but
                                                  // far
                                                  // away
                                                  // from
                                                  // that)
          .put("ysyl2", YSYLAgent2::createFromSpec).put("ysyl3", YSYLAgent3::createFromSpec)
          .put("ysyl4", YSYLAgent4::createFromSpec).put("nsyl2", NSYLAgent2::createFromSpec)
          .put("hbl", HBLAgent::createFromSpec) // HBL agents form a belief on whether a bid/ask
                                                // would be accepted with a probability
          .put("hbl2", HBLAgent2::createFromSpec) // HBL agents form a belief on whether a bid/ask
          // would be accepted with a probability
          .build();

  private static final Map<String, MarketCreator> marketNameMap =
      ImmutableMap.<String, MarketCreator>builder() //
          .put("cda", CdaMarket::createFromSpec) // CDA Market
          .put("call", CallMarket::createFromSpec) // Call Market
          .build();

  interface AgentCreator {

    Agent createAgent(Sim sim, Fundamental fundamental, Collection<Market> markets, Market market,
        Spec spec, Random rand);

  }

  interface MarketCreator {

    Market createMarket(Sim sim, Spec spec);

  }

}
