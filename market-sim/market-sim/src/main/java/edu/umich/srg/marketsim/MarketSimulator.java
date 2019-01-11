package edu.umich.srg.marketsim;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.event.EventQueue;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class MarketSimulator implements Sim {

  private final Collection<Market> markets;
  private final Collection<Agent> agents;
  private final Fundamental fundamental;
  private final Features features;
  private final EventQueue eventQueue;

  private Map<Agent, ? extends AgentInfo> agentPayoffs;

  // FIXME Still need SIP

  private MarketSimulator(Fundamental fundamental, Random rand) {
    this.fundamental = fundamental;
    this.features = new Features();
    this.markets = new ArrayList<>();
    this.agents = new ArrayList<>();
    this.eventQueue = new EventQueue(rand);

    this.agentPayoffs = null;
  }

  public static MarketSimulator create(Fundamental fundamental, Random rand) {
    return new MarketSimulator(fundamental, rand);
  }

  /**
   * Call to initialize all events before starting the simulation. Usually initialization is setting
   * their first arrival.
   */
  public void initialize() {
    for (Agent agent : agents) {
      agent.initialize();
    }
  }

  public void executeUntil(TimeStamp finalTime) {
    eventQueue.executeUntil(finalTime);
  }

  public Market addMarket(Market market) {
    markets.add(market);
    return market;
  }

  public void addAgent(Agent agent) {
    agents.add(agent);
  }

  public JsonObject computeFeatures() {
    return features.computeFeatures(this);
  }

  public Collection<Market> getMarkets() {
    return Collections.unmodifiableCollection(markets);
  }

  public Collection<Agent> getAgents() {
    return Collections.unmodifiableCollection(agents);
  }

  public Fundamental getFundamental() {
    return fundamental;
  }

  /** Get the payoffs of every agent in the simuation. */
  public Map<Agent, ? extends AgentInfo> getAgentPayoffs() {
    if (agentPayoffs == null) {
      // Get total agent holdings and profit according to all markets
      Map<Agent, SimAgentInfo> payoffs = Maps.toMap(agents, a -> new SimAgentInfo());
      agentPayoffs = payoffs;

      for (Market market : markets) {
        for (Entry<Agent, AgentInfo> e : market.getAgentInfo()) {
          SimAgentInfo info = payoffs.get(e.getKey());
          info.holdings += e.getValue().getHoldings();
          info.profit += e.getValue().getProfit();
          info.submissions += e.getValue().getSubmissions();
        }
      }

      // Get current fundamental price
      double fundamentalValue = fundamental.getValueAt(getCurrentTime()).doubleValue();
      for (Entry<Agent, SimAgentInfo> e : payoffs.entrySet()) {
        e.getValue().profit += e.getValue().holdings * fundamentalValue
            + e.getKey().payoffForPosition(e.getValue().holdings);
      }
    }
    return agentPayoffs;
  }

  @Override
  public void scheduleIn(TimeStamp delay, Runnable activity) {
    eventQueue.scheduleActivityIn(delay, activity);
  }

  @Override
  public TimeStamp getCurrentTime() {
    return eventQueue.getCurrentTime();
  }

  @Override
  public void addFeature(String name, double value) {
    features.accept(name, value);
  }

  private static class SimAgentInfo implements AgentInfo {
    private double profit;
    private int holdings;
    private int submissions;

    private SimAgentInfo() {
      this.profit = 0;
      this.holdings = 0;
      this.submissions = 0;
    }

    @Override
    public double getProfit() {
      return profit;
    }

    @Override
    public int getHoldings() {
      return holdings;
    }

    @Override
    public int getSubmissions() {
      return submissions;
    }

  }

}
