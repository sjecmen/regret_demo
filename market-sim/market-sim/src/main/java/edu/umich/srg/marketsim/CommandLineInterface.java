package edu.umich.srg.marketsim;

import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.JsonObject;

import edu.umich.srg.distributions.Uniform;
import edu.umich.srg.egtaonline.Observation;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.Runner;
import edu.umich.srg.egtaonline.SimSpec;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.marketsim.EntityBuilder.AgentCreator;
import edu.umich.srg.marketsim.EntityBuilder.MarketCreator;
import edu.umich.srg.marketsim.Keys.FundamentalMean;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockProb;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.RandomSeed;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.fundamental.GaussianMeanReverting;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.util.PositionalSeed;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CommandLineInterface {

  private static final String keyPrefix = "edu.umich.srg.marketsim.Keys$";
  private static final CaseFormat keyCaseFormat = CaseFormat.LOWER_CAMEL;
  private static final Splitter specSplitter = Splitter.on('_').omitEmptyStrings();

  public static void main(String[] args) throws IOException {
    Runner.run(CommandLineInterface::simulate, args, keyPrefix, keyCaseFormat);
  }

  /**
   * Run the market-sim simulation with a given simspec, log, and observation number. This is the
   * main entry point for executing the simulator from java.
   * 
   * @param simNum This is the unique observation number for one simulation, not the number of
   *        observations to produce. This method returns a single observation.
   */
  public static Observation simulate(SimSpec spec, int simNum) {
    Spec configuration = spec.configuration.withDefault(Keys.DEFAULT_KEYS);
    long seed = PositionalSeed.with(configuration.get(RandomSeed.class)).getSeed(simNum);
    Random rand = new Random(seed);

    Fundamental fundamental = GaussianMeanReverting.create(new Random(rand.nextLong()),
        configuration.get(FundamentalMean.class), configuration.get(FundamentalMeanReversion.class),
        configuration.get(FundamentalShockVar.class),
        configuration.get(FundamentalShockProb.class));
    MarketSimulator sim = MarketSimulator.create(fundamental, new Random(rand.nextLong()));

    List<Market> markets = addMarkets(sim, spec.configuration.get(Markets.class), configuration);
    List<PlayerInfo> playerInfo =
        addPlayers(sim, fundamental, spec.assignment, markets, configuration, rand.nextLong());

    sim.initialize();
    sim.executeUntil(TimeStamp.of(configuration.get(SimLength.class)));

    // Update player observations
    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    for (PlayerInfo info : playerInfo) {
      info.payoff = payoffs.get(info.agent).getProfit();
      info.features = info.agent.getFeatures();
    }

    return new Observation() {

      @Override
      public Collection<? extends Player> getPlayers() {
        return playerInfo;
      }

      @Override
      public JsonObject getFeatures() {
        return sim.computeFeatures();
      }

    };
  }

  private static List<Market> addMarkets(MarketSimulator sim, Iterable<String> marketSpecs,
      Spec configuration) {
    ImmutableList.Builder<Market> marketBuilder = ImmutableList.builder();
    for (String stringSpec : marketSpecs) {
      MarketCreator creator = EntityBuilder.getMarketCreator(getType(stringSpec));
      Spec marketSpec = getSpec(stringSpec).withDefault(configuration);

      Market market = creator.createMarket(sim, marketSpec);
      sim.addMarket(market);
      marketBuilder.add(market);
    }
    return marketBuilder.build();
  }

  /**
   * In order for agent seeds to be identical independent of the order the agents were added in or
   * exactly how they were specified, the random seeds that are passed to the agents are generated
   * with a hash of the agent's specified strategy and reused for all agents that share the same
   * strategy.
   */
  /*
   * TODO Ideally the hash will be off of the agent type and the relevant aspect of the total spec
   * that it receives. However if we use the whole spec, then adding a different agent with
   * parameters in the spec will change the agents hash, and hence the random seeds. However,
   * hashing the strategy has the downside that agents that actually share the same relevant
   * specification may get different random seeds due to order or specifying information in the
   * global spec.
   */
  private static List<PlayerInfo> addPlayers(MarketSimulator sim, Fundamental fundamental,
      Multiset<RoleStrat> assignment, Collection<Market> markets, Spec configuration,
      long baseSeed) {
    Map<String, Random> randoms = new HashMap<>();
    PositionalSeed seed = PositionalSeed.with(baseSeed);
    Uniform<Market> marketSelection = Uniform.over(markets);

    ImmutableList.Builder<PlayerInfo> playerInfoBuilder = ImmutableList.builder();
    for (Entry<RoleStrat> roleStratCounts : assignment.entrySet()) {
      String strategy = roleStratCounts.getElement().getStrategy();
      AgentCreator creator = EntityBuilder.getAgentCreator(getType(strategy));
      Spec agentSpec = getSpec(strategy).withDefault(configuration);
      Random rand = randoms.computeIfAbsent(strategy, s -> new Random(seed.getSeed(s.hashCode())));

      for (int i = 0; i < roleStratCounts.getCount(); ++i) {
        Agent agent = creator.createAgent(sim, fundamental, markets, marketSelection.sample(rand),
            agentSpec, new Random(rand.nextLong()));
        sim.addAgent(agent);
        playerInfoBuilder.add(new PlayerInfo(roleStratCounts.getElement(), agent));
      }
    }

    return playerInfoBuilder.build();
  }

  private static String getType(String strategy) {
    int index = strategy.indexOf(':');
    return (index < 0 ? strategy : strategy.substring(0, index)).toLowerCase();
  }

  private static Spec getSpec(String strategy) {
    int index = strategy.indexOf(':');
    if (index < 0) {
      return Spec.empty();
    } else {
      return Spec.fromPairs(keyPrefix, keyCaseFormat,
          specSplitter.split(strategy.substring(index + 1)));
    }
  }

  private static class PlayerInfo implements Player {

    private final String role;
    private final String strategy;
    private final Agent agent;
    private double payoff;
    private JsonObject features;

    private PlayerInfo(RoleStrat roleAndStrategy, Agent agent) {
      this.role = roleAndStrategy.getRole();
      this.strategy = roleAndStrategy.getStrategy();
      this.agent = agent;
      this.payoff = 0;
      this.features = null;
    }

    @Override
    public String getRole() {
      return role;
    }

    @Override
    public String getStrategy() {
      return strategy;
    }

    @Override
    public double getPayoff() {
      return payoff;
    }

    @Override
    public JsonObject getFeatures() {
      return features;
    }

    @Override
    public String toString() {
      return role + ": " + strategy + " (" + payoff + ") " + features;
    }

  }

}
