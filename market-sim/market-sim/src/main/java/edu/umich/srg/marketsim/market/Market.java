package edu.umich.srg.marketsim.market;

import com.google.gson.JsonObject;

import edu.umich.srg.fourheap.FourHeap;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.fundamental.Fundamental;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

public interface Market {

  MarketView getView(Agent agent, TimeStamp latency);

  /** Get zero latency view. */
  default MarketView getView(Agent agent) {
    return getView(agent, TimeStamp.ZERO);
  }

  Iterable<Entry<Agent, AgentInfo>> getAgentInfo();

  JsonObject getFeatures(Fundamental fundamental);

  interface MarketView {

    TimeStamp getLatency();

    OrderRecord submitOrder(OrderType buyOrSell, Price price, int quantity);

    void withdrawOrder(OrderRecord record, int quantity);

    default void withdrawOrder(OrderRecord record) {
      withdrawOrder(record, record.getQuantity());
    }

    Quote getQuote();

    Set<OrderRecord> getActiveOrders();

    double getProfit();

    int getHoldings();

    int getSubmissions();

    // TODO Add last transaction price, maybe notification

  }

  interface AgentInfo {

    double getProfit();

    int getHoldings();

    int getSubmissions();

  }

  class ImmutableAgentInfo implements AgentInfo, Serializable {

    private static final ImmutableAgentInfo empty = new ImmutableAgentInfo(0, 0, 0);

    private final double profit;
    private final int holdings;
    private final int submissions;

    private ImmutableAgentInfo(double profit, int holdings, int submissions) {
      this.profit = profit;
      this.holdings = holdings;
      this.submissions = submissions;
    }

    public static ImmutableAgentInfo empty() {
      return empty;
    }

    public static ImmutableAgentInfo of(double profit, int holdings, int submissions) {
      return new ImmutableAgentInfo(profit, holdings, submissions);
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

    private static final long serialVersionUID = 1;

  }

  interface OrderInfo {

    Price getPrice();

    int getQuantity();

    OrderType getOrderType();
  }

  FourHeap<Price> getOrderBook();

}
