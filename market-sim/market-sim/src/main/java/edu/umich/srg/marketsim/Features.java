package edu.umich.srg.marketsim;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.AgentInfo;
import edu.umich.srg.util.SummStats;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

class Features {
  private final Map<String, SummStats> summaryFeatures;

  Features() {
    this.summaryFeatures = new HashMap<>();
  }

  void accept(String name, double value) {
    SummStats stats = summaryFeatures.get(name);
    if (stats == null) {
      stats = SummStats.empty();
      summaryFeatures.put(name, stats);
    }
    stats.accept(value);
  }

  JsonObject computeFeatures(MarketSimulator simulator) {
    JsonObject features = new JsonObject();

    // Summary features
    for (Entry<String, SummStats> entry : summaryFeatures.entrySet()) {
      String key = entry.getKey().toLowerCase().replace(' ', '_');
      features.addProperty(key + "_c", entry.getValue().getCount());
      features.addProperty(key + "_a", entry.getValue().getAverage());
    }

    // Fundamental features
    Fundamental fundamental = simulator.getFundamental();
    features.add("fundamental",
        convertSparseData(fundamental.getFundamentalValues(simulator.getCurrentTime())));

    // Market features
    for (Market market : simulator.getMarkets()) {
      features.add("cda", market.getFeatures(fundamental));
    }

    // Surplus Features
    surplusFeatures(simulator.getAgentPayoffs(), features);

    calcBiasResults(simulator.getAgents(), features);
    return features;
  }

  private static JsonArray convertSparseData(
      Iterable<? extends Sparse.Entry<? extends Number>> data) {
    JsonArray json = new JsonArray();
    for (Sparse.Entry<? extends Number> obs : data) {
      JsonArray point = new JsonArray();
      point.add(new JsonPrimitive(obs.getIndex()));
      point.add(new JsonPrimitive(obs.getElement().doubleValue()));
      json.add(point);
    }
    return json;
  }

  private static void surplusFeatures(Map<Agent, ? extends AgentInfo> results,
      JsonObject features) {
    double surplus = results.values().stream().mapToDouble(AgentInfo::getProfit).sum();
    // By default, competitive equilibrium is assuming infinite opportunities to trade.
    CompEqResults compEq = calcCompetitiveEquilibrium(results, a -> Integer.MAX_VALUE);
    // For completeness, we also compute it with submission limits
    CompEqResults compEqSub =
        calcCompetitiveEquilibrium(results, a -> results.get(a).getSubmissions());

    // Assert results are consistent
    assert Math.abs(compEq.maxSurplus - compEq.imSurplusLoss - compEq.emSurplusLoss
        - surplus) < 1e-6 : "surplus losses didn't sum properly";
    assert Math.abs(compEqSub.maxSurplus - compEqSub.imSurplusLoss - compEqSub.emSurplusLoss
        - surplus) < 1e-6 : "submission constrained surplus losses didn't sum properly";

    assert surplus <= compEqSub.maxSurplus
        + 1e6 : "surplus was greater than constrained max surplus";
    assert compEqSub.maxSurplus <= compEq.maxSurplus
        + 1e6 : "submission constrained max surplus wasn't less than unconstrained max surplus";

    // Record features
    features.addProperty("total_surplus", surplus);

    features.addProperty("max_surplus", compEq.maxSurplus);
    features.addProperty("effeciency_ratio", surplus / compEq.maxSurplus);
    features.addProperty("ce_price", compEq.cePrice);
    features.addProperty("im_surplus_loss", compEq.imSurplusLoss);
    features.addProperty("em_surplus_loss", compEq.emSurplusLoss);

    features.addProperty("max_surplus_sublim", compEqSub.maxSurplus);
    features.addProperty("ce_price_sublim", compEqSub.cePrice);
    features.addProperty("im_surplus_loss_sublim", compEqSub.imSurplusLoss);
    features.addProperty("em_surplus_loss_sublim", compEqSub.emSurplusLoss);
  }

  private static double calcDesiredBenefit(OrderType type, Agent agent,
      Map<Agent, Integer> positionMap) {
    int position = positionMap.get(agent);
    return agent.payoffForPosition(position + type.sign()) - agent.payoffForPosition(position);
  }

  // This will utterly fail if private valuations aren't diminishing marginal
  // This uses payoffForPosition which scales linearly. The linear number should be small
  // enough that it doesn't really matter, but it is a source of inefficiency / slowness.
  private static CompEqResults calcCompetitiveEquilibrium(Map<Agent, ? extends AgentInfo> results,
      ToIntFunction<Agent> getMaxPosition) {
    // Editable Map holding positions in ce
    Map<Agent, Integer> cePositions =
        results.keySet().stream().collect(Collectors.toMap(e -> e, e -> 0));

    // Priority queues of buyers and sellers, ordered by the highest gain from a marginal trade
    Comparator<Entry<Agent, Double>> comp =
        Comparator.<Entry<Agent, Double>, Double>comparing(Entry::getValue).reversed();
    PriorityQueue<Entry<Agent, Double>> buyers = new PriorityQueue<>(comp);
    PriorityQueue<Entry<Agent, Double>> sellers = new PriorityQueue<>(comp);

    // These are necessary to get a clearing price when no agent transacts
    for (Entry<Agent, Integer> entry : cePositions.entrySet()) {
      Agent agent = entry.getKey();
      // Only include agents that can trade
      if (getMaxPosition.applyAsInt(agent) > 0) {
        buyers.add(new AbstractMap.SimpleEntry<>(agent,
            agent.payoffForPosition(1) - agent.payoffForPosition(0)));
        sellers.add(new AbstractMap.SimpleEntry<>(agent,
            agent.payoffForPosition(-1) - agent.payoffForPosition(0)));
      }
    }

    double cePrice = 0; // Any price is valid if no one trades
    double maxSurplus = 0;
    while (!buyers.isEmpty() && !sellers.isEmpty()
        && buyers.peek().getValue() + sellers.peek().getValue() > 0) {

      // Extract agents
      Entry<Agent, Double> buyerEnt = buyers.poll();
      Agent buyer = buyerEnt.getKey();
      double buyerVal = buyerEnt.getValue();

      Entry<Agent, Double> sellerEnt = sellers.poll();
      Agent seller = sellerEnt.getKey();
      double sellerVal = sellerEnt.getValue();

      // Update values
      cePrice = (buyerVal - sellerVal) / 2;
      maxSurplus += buyerVal + sellerVal;

      // Update positions
      int buyerPos = cePositions.compute(buyer, (key, val) -> val + 1);
      int sellerPos = cePositions.compute(seller, (key, val) -> val - 1);

      // Put back in priority queue with updated values if they can still trade
      if (buyerPos < getMaxPosition.applyAsInt(buyer)) {
        buyerEnt
            .setValue(buyer.payoffForPosition(buyerPos + 1) - buyer.payoffForPosition(buyerPos));
        buyers.add(buyerEnt);
      }
      if (-sellerPos < getMaxPosition.applyAsInt(seller)) {
        sellerEnt.setValue(
            seller.payoffForPosition(sellerPos - 1) - seller.payoffForPosition(sellerPos));
        sellers.add(sellerEnt);
      }

      // Make sure queues are up to date
      double desiredBenefit;
      while ((desiredBenefit =
          calcDesiredBenefit(BUY, buyers.peek().getKey(), cePositions)) != buyers.peek()
              .getValue()) {
        buyerEnt = buyers.poll();
        buyerEnt.setValue(desiredBenefit);
        buyers.add(buyerEnt);
      }
      while ((desiredBenefit =
          calcDesiredBenefit(SELL, sellers.peek().getKey(), cePositions)) != sellers.peek()
              .getValue()) {
        sellerEnt = sellers.poll();
        sellerEnt.setValue(desiredBenefit);
        sellers.add(sellerEnt);
      }
    }

    // Calculate surplus loss based off of trade differences
    double imSurplusLoss = 0;
    double emSurplusLoss = 0;
    for (Entry<Agent, ? extends AgentInfo> result : results.entrySet()) {
      Agent agent = result.getKey();
      int cePosition = cePositions.get(agent);
      int actualPosition = result.getValue().getHoldings();

      if (cePosition == actualPosition) { // No loss
        continue;
      }

      int posDiff = actualPosition - cePosition;
      double surplusLoss = agent.payoffForPosition(cePosition)
          - agent.payoffForPosition(actualPosition) + posDiff * cePrice;
      if (cePosition * posDiff >= 0) { // EM Trader
        emSurplusLoss += surplusLoss;
      } else { // IM Trader
        imSurplusLoss += surplusLoss;
      }
    }

    return new CompEqResults(cePrice, maxSurplus, imSurplusLoss, emSurplusLoss);
  }

  private void calcBiasResults(Collection<Agent> agents, JsonObject features) {
    SummStats beliefStats = SummStats.empty();
    SummStats submissionStats = SummStats.empty();
    for (Agent agent : agents) {
      if (!agent.getBeliefBias().isEmpty()) {
        beliefStats.accept(agent.getBeliefBias());
      }
      if (!agent.getSubmissionDist().isEmpty()) {
        submissionStats.accept(agent.getSubmissionDist());
      }
    }

    features.addProperty("estimateMean", beliefStats.getAverage());
    features.addProperty("estimateVar", beliefStats.getVariance());
    features.addProperty("submissionMean", submissionStats.getAverage());
    features.addProperty("submissionVar", submissionStats.getVariance());
  }

  private static final class CompEqResults {
    private final double cePrice;
    private final double maxSurplus;
    private final double imSurplusLoss;
    private final double emSurplusLoss;

    private CompEqResults(double cePrice, double maxSurplus, double imSurplusLoss,
        double emSurplusLoss) {
      assert maxSurplus >= 0 : "max surplus was negative";
      assert !Double.isNaN(maxSurplus) : "max surplus was nan";
      assert !Double.isNaN(imSurplusLoss) : "im surplus loss was nan";
      assert !Double.isNaN(emSurplusLoss) : "em surplus loss was nan";

      this.cePrice = cePrice;
      this.maxSurplus = maxSurplus;
      this.imSurplusLoss = imSurplusLoss;
      this.emSurplusLoss = emSurplusLoss;
    }
  }
}
