package edu.umich.srg.marketsim;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.CaseFormat;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Test;

import edu.umich.srg.egtaonline.Observation;
import edu.umich.srg.egtaonline.Observation.Player;
import edu.umich.srg.egtaonline.Runner;
import edu.umich.srg.egtaonline.SimSpec;
import edu.umich.srg.egtaonline.SimSpec.RoleStrat;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.egtaonline.spec.Value;
import edu.umich.srg.marketsim.Keys.ArrivalRate;
import edu.umich.srg.marketsim.Keys.FundamentalMeanReversion;
import edu.umich.srg.marketsim.Keys.FundamentalShockVar;
import edu.umich.srg.marketsim.Keys.Markets;
import edu.umich.srg.marketsim.Keys.RandomSeed;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.agent.NoiseAgent;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class IntegrationTest {

  private static final Random rand = new Random();
  private static final Gson gson = new Gson();
  private static final Joiner stratJoiner = Joiner.on('_');
  private static final String keyPrefix = "edu.umich.srg.marketsim.Keys$";
  private static final CaseFormat keyCaseFormat = CaseFormat.LOWER_CAMEL;
  private static final double tol = 1e-8;

  @Test
  public void simpleMinimalTest() {
    int numAgents = 10;

    MarketSimulator sim = MarketSimulator.create(ConstantFundamental.create(0), rand);
    Market cda = sim.addMarket(CdaMarket.create(sim));
    for (int i = 0; i < numAgents; ++i)
      sim.addAgent(new NoiseAgent(sim, cda, Spec.fromPairs(ArrivalRate.class, 0.5), rand));
    sim.initialize();
    sim.executeUntil(TimeStamp.of(10));

    Map<Agent, ? extends AgentInfo> payoffs = sim.getAgentPayoffs();
    assertEquals(numAgents, payoffs.size());
    assertEquals(0, payoffs.values().stream().mapToDouble(AgentInfo::getProfit).sum(), tol);
    assertTrue(payoffs.values().stream().mapToInt(AgentInfo::getSubmissions).allMatch(s -> s >= 0));
  }

  @Test
  public void simpleSpecTest() {
    int numAgents = 10;
    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);

    Observation obs = CommandLineInterface.simulate(spec, 0);

    Iterable<? extends Player> players = obs.getPlayers();
    assertEquals(numAgents, Iterables.size(players));
    assertEquals(0,
        StreamSupport.stream(players.spliterator(), false).mapToDouble(p -> p.getPayoff()).sum(),
        tol);
  }

  @Test
  public void simpleEgtaTest() {
    int numAgents = 10;
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 1, true, keyPrefix,
        keyCaseFormat);

    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertFalse(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertFalse(player.getAsJsonObject().has("features"));
  }

  /**
   * Test that the the multi job pipeline does the correct thing. This is only run with one
   * observation, so the multiple threads don't actually do anything.
   */
  @Test
  public void multiThreadEgtaTest() {
    int numAgents = 10;
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 2, true, keyPrefix,
        keyCaseFormat);

    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertFalse(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertFalse(player.getAsJsonObject().has("features"));
  }

  @Test
  public void simpleFullTest() {
    int numAgents = 10;
    StringWriter obsData = new StringWriter();

    Spec agentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec configuration = Spec.fromPairs(SimLength.class, 10l, Markets.class,
        ImmutableList.of("cda"), FundamentalMeanReversion.class, 0d, FundamentalShockVar.class, 0d);

    Multiset<RoleStrat> assignment = HashMultiset.create(1);
    assignment.add(RoleStrat.of("role", toStratString("noise", agentSpec)), numAgents);
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);


    Runner.run(CommandLineInterface::simulate, specReader, obsData, 1, 1, 1, false, keyPrefix,
        keyCaseFormat);

    assertFalse(obsData.toString().isEmpty());

    JsonObject observation = gson.fromJson(obsData.toString(), JsonObject.class);
    assertTrue(observation.has("features"));
    assertTrue(observation.has("players"));
    assertEquals(10, observation.getAsJsonArray("players").size());
    assertEquals(0,
        StreamSupport.stream(observation.getAsJsonArray("players").spliterator(), false)
            .mapToDouble(p -> p.getAsJsonObject().getAsJsonPrimitive("payoff").getAsDouble()).sum(),
        tol);
    for (JsonElement player : observation.getAsJsonArray("players"))
      assertTrue(player.getAsJsonObject().has("features"));
  }

  /**
   * Tests to see if identical simulations with the same random seed produce the same result. We
   * can't arbitrarily order the agents, because different entry orders in the event queue will
   * produce different scheduling, and hence, slightly different results.
   */
  @Test
  public void identicalRandomTest() {
    int numAgentAs = 10, numAgentBs = 5;
    long seed = rand.nextLong();

    StringWriter obsData = new StringWriter();

    Spec aAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec bAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.8);
    Spec configuration = Spec.builder().put(SimLength.class, 10l)
        .put(Markets.class, ImmutableList.of("cda")).put(FundamentalMeanReversion.class, 0d)
        .put(FundamentalShockVar.class, 0d).put(RandomSeed.class, seed).build();

    Multiset<RoleStrat> assignment = ImmutableMultiset.<RoleStrat>builder()
        .addCopies(RoleStrat.of("role", toStratString("noise", aAgentSpec)), numAgentAs)
        .addCopies(RoleStrat.of("role", toStratString("noise", bAgentSpec)), numAgentBs).build();
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);

    // Run the simulation once
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false, keyPrefix,
        keyCaseFormat);

    // Save the results
    Iterator<JsonObject> obs1s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);


    // Run the simulation again
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false, keyPrefix,
        keyCaseFormat);

    // Save the results
    Iterator<JsonObject> obs2s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Verify identical output for players
    // If this fails, that does't mean they weren't identical, but more care will need to be taken
    // for the comparison
    while (obs1s.hasNext() && obs2s.hasNext()) {
      JsonObject obs1 = obs1s.next();
      JsonObject obs2 = obs2s.next();

      assertEquals(obs1.get("players"), obs2.get("players"));

      JsonObject obs1Features = obs1.get("features").getAsJsonObject();
      JsonObject obs2Features = obs2.get("features").getAsJsonObject();
      JsonObject obs1Market = removeMarketFeatures(obs1Features);
      JsonObject obs2Market = removeMarketFeatures(obs2Features);

      assertEquals(obs1Features, obs2Features);
      assertEquals(obs1Market, obs2Market);
    }

    assertFalse("Simulation 1 had more observations", obs1s.hasNext());
    assertFalse("Simulation 2 had more observations", obs2s.hasNext());
  }

  /** Tests that num jobs doesn't change order of observations. */
  @Test
  public void raceConditionTest() {
    int numAgentAs = 10, numAgentBs = 5;
    long seed = rand.nextLong();

    StringWriter obsData = new StringWriter();

    Spec aAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec bAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.8);
    Spec configuration = Spec.builder().put(SimLength.class, 10l)
        .put(Markets.class, ImmutableList.of("cda")).put(FundamentalMeanReversion.class, 0d)
        .put(FundamentalShockVar.class, 0d).put(RandomSeed.class, seed).build();

    Multiset<RoleStrat> assignment = ImmutableMultiset.<RoleStrat>builder()
        .addCopies(RoleStrat.of("role", toStratString("noise", aAgentSpec)), numAgentAs)
        .addCopies(RoleStrat.of("role", toStratString("noise", bAgentSpec)), numAgentBs).build();
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);

    // Run the simulation once with one job
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 1, false, keyPrefix,
        keyCaseFormat);

    // Save the results
    Iterator<JsonObject> obs1s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);


    // Run the simulation again with two jobs
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 10, 1, 2, false, keyPrefix,
        keyCaseFormat);

    // Save the results
    Iterator<JsonObject> obs2s = Arrays.stream(obsData.toString().split("\n"))
        .map(line -> gson.fromJson(line, JsonObject.class)).iterator();

    // Verify identical output for players
    // If this fails, that does't mean they weren't identical, but more care will need to be taken
    // for the comparison
    while (obs1s.hasNext() && obs2s.hasNext()) {
      JsonObject obs1 = obs1s.next();
      JsonObject obs2 = obs2s.next();

      assertEquals(obs1.get("players"), obs2.get("players"));

      JsonObject obs1Features = obs1.get("features").getAsJsonObject();
      JsonObject obs2Features = obs2.get("features").getAsJsonObject();
      JsonObject obs1Market = removeMarketFeatures(obs1Features);
      JsonObject obs2Market = removeMarketFeatures(obs2Features);

      assertEquals(obs1Features, obs2Features);
      assertEquals(obs1Market, obs2Market);
    }

    assertFalse("Simulation 1 had more observations", obs1s.hasNext());
    assertFalse("Simulation 2 had more observations", obs2s.hasNext());
  }

  /** Tests that simsPerObs appropriately aggregates information. */
  @Test
  public void simsPerObsTest() {
    int numAgentAs = 10, numAgentBs = 5;
    long seed = rand.nextLong();

    StringWriter obsData = new StringWriter();

    Spec aAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.5);
    Spec bAgentSpec = Spec.fromPairs(ArrivalRate.class, 0.8);
    Spec configuration = Spec.builder().put(SimLength.class, 10l)
        .put(Markets.class, ImmutableList.of("cda")).put(FundamentalMeanReversion.class, 0d)
        .put(FundamentalShockVar.class, 0d).put(RandomSeed.class, seed).build();

    Multiset<RoleStrat> assignment = ImmutableMultiset.<RoleStrat>builder()
        .addCopies(RoleStrat.of("role", toStratString("noise", aAgentSpec)), numAgentAs)
        .addCopies(RoleStrat.of("role", toStratString("noise", bAgentSpec)), numAgentBs).build();
    SimSpec spec = SimSpec.create(assignment, configuration);
    Reader specReader = toReader(spec);

    // Run the simulation ten times
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 20, 1, 1, true, keyPrefix,
        keyCaseFormat);

    // Save the average payoff per role strategy, the only thing that can be guaranteed
    List<Map<RoleStrat, DoubleSummaryStatistics>> payoffsNormal =
        aggregateStringObservations(obsData.toString(), 10);

    // Reset
    obsData = new StringWriter();
    specReader = toReader(spec);

    // Run the simulation again but only return one aggregate observation
    Runner.run(CommandLineInterface::simulate, specReader, obsData, 2, 10, 1, true, keyPrefix,
        keyCaseFormat);

    List<Map<RoleStrat, DoubleSummaryStatistics>> payoffsObsPerSim =
        aggregateStringObservations(obsData.toString(), 1);

    // Verify identical mean payoffs for role and strategy
    Iterator<Map<RoleStrat, DoubleSummaryStatistics>> it1 = payoffsNormal.iterator();
    Iterator<Map<RoleStrat, DoubleSummaryStatistics>> it2 = payoffsObsPerSim.iterator();
    while (it1.hasNext() && it2.hasNext()) {
      Map<RoleStrat, DoubleSummaryStatistics> m1 = it1.next();
      Map<RoleStrat, DoubleSummaryStatistics> m2 = it2.next();

      assertEquals("Both maps don't have the same keys", m1.keySet(), m2.keySet());
      for (Map.Entry<RoleStrat, DoubleSummaryStatistics> entry : m1.entrySet()) {
        assertEquals(entry.getValue().getAverage(), m2.get(entry.getKey()).getAverage(),
            Math.max(Math.abs(tol * entry.getValue().getAverage()), tol));
        assertEquals(entry.getValue().getCount(), m2.get(entry.getKey()).getCount() * 10);
      }
    }

    // Check that iterators are the same length
    assertFalse(it1.hasNext());
    assertFalse(it2.hasNext());
  }

  // TODO Test that agents inherit specifications from configuration

  // TODO Test that invalid agent names throw exception

  private static JsonObject removeMarketFeatures(JsonObject features) {
    String marketName = features.entrySet().stream().map(Map.Entry::getKey)
        .filter(k -> k.startsWith("cda_")).findAny().get();
    return features.remove(marketName).getAsJsonObject();
  }

  private static String toStratString(String name, Spec spec) {
    StringBuilder strat = new StringBuilder(name).append(':');
    stratJoiner.appendTo(strat,
        spec.entrySet().stream()
            .map(e -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, e.getKey().getSimpleName())
                + '_' + e.getValue())
            .iterator());
    return strat.toString();
  }

  private static Reader toReader(SimSpec spec) {
    JsonObject json = new JsonObject();

    JsonObject assignment = new JsonObject();
    for (Entry<RoleStrat> entry : spec.assignment.entrySet()) {
      String role = entry.getElement().getRole();
      JsonElement strategies = assignment.get(role);
      if (strategies == null) {
        strategies = new JsonObject();
        assignment.add(role, strategies);
      }
      strategies.getAsJsonObject().addProperty(entry.getElement().getStrategy(), entry.getCount());
    }
    json.add("assignment", assignment);

    JsonObject configuration = new JsonObject();
    for (Map.Entry<Class<? extends Value<?>>, Value<?>> entry : spec.configuration.entrySet()) {
      configuration.addProperty(
          CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entry.getKey().getSimpleName()),
          entry.getValue().toString());
    }
    json.add("configuration", configuration);

    return new StringReader(json.toString());
  }

  private static List<Map<RoleStrat, DoubleSummaryStatistics>> aggregateStringObservations(
      String observations, int perGroup) {
    // Save the average payoff per role strategy, the only thing that can be guaranteed
    return Lists
        // Group by 10
        .partition(Arrays.asList(observations.split("\n")), perGroup).stream()
        // Aggregate each group of 10
        .map(
            // Turn all observations into a stream of player JsonObjects
            obsStrings -> obsStrings.stream().map(line -> gson.fromJson(line, JsonObject.class))
                .flatMap(obs -> StreamSupport
                    .stream(obs.get("players").getAsJsonArray().spliterator(), false))
                .map(JsonElement::getAsJsonObject)
                // Collect into map to a double summary stats object
                .collect(Collectors.groupingBy(
                    p -> RoleStrat.of(p.get("role").getAsString(), p.get("strategy").getAsString()),
                    Collectors.summarizingDouble(p -> p.get("payoff").getAsDouble()))))
        .collect(Collectors.toList());
  }

}
