package edu.umich.srg.marketsim.market;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MarketAsserts;
import edu.umich.srg.marketsim.testing.MockAgent;
import edu.umich.srg.marketsim.testing.MockSim;

public class CDAMarketTest {
  private MockSim sim;
  private CdaMarket market;
  private MockAgent agent;
  private MarketView view;

  @Before
  public void setup() {
    sim = new MockSim();
    market = CdaMarket.create(sim);
    agent = new MockAgent();
    view = market.getView(agent, TimeStamp.ZERO);
  }

  @Test
  public void addBid() {
    view.submitOrder(BUY, Price.of(1), 1);
    MarketAsserts.assertQuote(view.getQuote(), Price.of(1), null);
  }

  @Test
  public void addAsk() {
    view.submitOrder(SELL, Price.of(1), 1);
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(1));
  }

  @Test
  public void withdrawTest() {
    OrderRecord order = view.submitOrder(SELL, Price.of(100), 1);
    MarketAsserts.assertQuote(view.getQuote(), null, Price.of(100));

    view.withdrawOrder(order, order.quantity);
    MarketAsserts.assertQuote(view.getQuote(), null, null);
  }

  @Test
  public void bidFirst() {
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);
    OrderRecord sell = view.submitOrder(SELL, Price.of(50), 1);

    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(Price.of(100), agent.lastTransactionPrice);
    assertTrue(view.getActiveOrders().isEmpty());
    assertEquals(0, buy.getQuantity());
    assertEquals(0, sell.getQuantity());
  }

  @Test
  public void askFirst() {
    OrderRecord sell = view.submitOrder(SELL, Price.of(50), 1);
    OrderRecord buy = view.submitOrder(BUY, Price.of(100), 1);

    MarketAsserts.assertQuote(view.getQuote(), null, null);
    assertEquals(Price.of(50), agent.lastTransactionPrice);
    assertTrue(view.getActiveOrders().isEmpty());
    assertEquals(0, buy.getQuantity());
    assertEquals(0, sell.getQuantity());
  }
}
