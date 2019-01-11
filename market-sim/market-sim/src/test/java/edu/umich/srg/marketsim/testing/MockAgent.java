package edu.umich.srg.marketsim.testing;

import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.agent.Agent;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.privatevalue.PrivateValue;
import edu.umich.srg.marketsim.privatevalue.PrivateValues;

public class MockAgent implements Agent {

  public final PrivateValue privateValue;
  public int transactions;
  public int transactedUnits;
  public Price lastTransactionPrice;

  private MockAgent(PrivateValue privateValue) {
    this.privateValue = privateValue;
    this.transactions = 0;
    this.lastTransactionPrice = null;
  }

  public MockAgent() {
    this(PrivateValues.noPrivateValue());
  }

  public static MockAgent create() {
    return builder().build();
  }

  public static MockAgentBuilder builder() {
    return new MockAgentBuilder();
  }

  @Override
  public void initialize() {}

  @Override
  public JsonObject getFeatures() {
    return new JsonObject();
  }

  @Override
  public double payoffForPosition(int position) {
    return privateValue.valueAtPosition(position);
  }

  @Override
  public void notifyOrderSubmitted(OrderRecord order) {}

  @Override
  public void notifyOrderWithrawn(OrderRecord order, int quantity) {}

  @Override
  public void notifyOrderTransacted(OrderRecord order, Price price, int quantity) {
    transactions++;
    transactedUnits += quantity;
    lastTransactionPrice = price;
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {}

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {}

  public static class MockAgentBuilder {

    private PrivateValue privateValue;

    private MockAgentBuilder() {
      privateValue = PrivateValues.noPrivateValue();
    }

    public MockAgentBuilder privateValue(PrivateValue privateValue) {
      this.privateValue = privateValue;
      return this;
    }

    public MockAgent build() {
      return new MockAgent(privateValue);
    }

  }

  @Override
  public void notifyOrderSubmittedToMarket(MarketView market, OrderNotification notification) {
    // TODO Auto-generated method stub

  }

  @Override
  public void notifyOrderWithdrawnFromMarket(MarketView market, OrderNotification notification) {
    // TODO Auto-generated method stub

  }

}
