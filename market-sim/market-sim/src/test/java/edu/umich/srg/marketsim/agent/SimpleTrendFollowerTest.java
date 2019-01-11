package edu.umich.srg.marketsim.agent;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;
import static edu.umich.srg.marketsim.testing.MarketAsserts.ABSENT;
import static edu.umich.srg.marketsim.testing.MarketAsserts.assertQuote;

import org.junit.Before;
import org.junit.Test;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys;
import edu.umich.srg.marketsim.Keys.Expiration;
import edu.umich.srg.marketsim.Keys.ProfitDemanded;
import edu.umich.srg.marketsim.Keys.TrendLength;
import edu.umich.srg.marketsim.MarketSimulator;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.ConstantFundamental;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.CdaMarket;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.testing.MockAgent;

import java.util.Random;

public class SimpleTrendFollowerTest {

  public static final Random rand = new Random();
  public static final Spec spec = Spec.fromDefaultPairs(Keys.DEFAULT_KEYS, TrendLength.class, 3,
      ProfitDemanded.class, 30, Expiration.class, TimeStamp.of(1000));

  private MarketSimulator sim;
  private Market market;
  private MarketView view;

  @Before
  public void setup() {
    Fundamental fundamental = ConstantFundamental.create(1e9);
    sim = MarketSimulator.create(fundamental, rand);
    market = sim.addMarket(CdaMarket.create(sim));
    Agent mockAgent = new MockAgent();
    view = market.getView(mockAgent, TimeStamp.ZERO);
  }

  /** No order is submitted if there's nothing to take first. */
  @Test
  public void buyNullAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addTransaction(10);
    addTransaction(20);
    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, ABSENT);
  }

  @Test
  public void sellNullBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addTransaction(90);
    addTransaction(80);
    addTransaction(70);

    assertQuote(view.getQuote(), ABSENT, ABSENT);
  }

  /** Something to take, but only one quote in market, so unbounded range for rebid. */
  @Test
  public void buyNullFollowupAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 40);
    addTransaction(10);
    addTransaction(20);
    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, 70);
  }

  @Test
  public void sellNullFollowupBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(BUY, 60);
    addTransaction(90);
    addTransaction(80);
    addTransaction(70);

    assertQuote(view.getQuote(), 30, ABSENT);
  }

  /** Equal prices still keep trend alive. */
  @Test
  public void buyEqualAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 40);
    addTransaction(10);
    addTransaction(20);
    addTransaction(20);

    assertQuote(view.getQuote(), ABSENT, 40);

    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, 70);
  }

  @Test
  public void sellEqualBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(BUY, 60);
    addTransaction(90);
    addTransaction(80);
    addTransaction(80);

    assertQuote(view.getQuote(), 60, ABSENT);

    addTransaction(70);

    assertQuote(view.getQuote(), 30, ABSENT);
  }

  /** Another order in the market, but it won't affect the price. */
  @Test
  public void buyUnboundedFollowupAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 40);
    addOrder(SELL, 80);
    addTransaction(10);
    addTransaction(20);
    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, 70);
  }

  @Test
  public void sellUnboundedFollowupBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(BUY, 20);
    addOrder(BUY, 60);
    addTransaction(90);
    addTransaction(80);
    addTransaction(70);

    assertQuote(view.getQuote(), 30, ABSENT);
  }

  /** Another order in the market, and we undercut them. */
  @Test
  public void buyBoundedFollowupAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 40);
    addOrder(SELL, 61);
    addTransaction(10);
    addTransaction(20);
    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, 60);
  }

  @Test
  public void sellBoundedFollowupBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(BUY, 39);
    addOrder(BUY, 60);
    addTransaction(90);
    addTransaction(80);
    addTransaction(70);

    assertQuote(view.getQuote(), 40, ABSENT);
  }

  /** Another order in the market at the same price, we can't undercut. */
  @Test
  public void buyUnconsumedAskTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 40);
    addOrder(SELL, 40);
    addTransaction(10);
    addTransaction(20);
    addTransaction(30);

    assertQuote(view.getQuote(), ABSENT, 40);
  }

  @Test
  public void sellUnconsumedBidTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(BUY, 60);
    addOrder(BUY, 60);
    addTransaction(90);
    addTransaction(80);
    addTransaction(70);

    assertQuote(view.getQuote(), 60, ABSENT);
  }

  /** Go almost to a trend, but then change direction. */
  @Test
  public void untriggeredSwitchTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 80);
    addOrder(BUY, 40);
    addTransaction(60);
    addTransaction(70);
    addTransaction(60);
    addTransaction(50);

    assertQuote(view.getQuote(), 10, 80);
  }

  /** Get a trend, then change direction. */
  @Test
  public void triggeredSwitchTest() {
    new SimpleTrendFollower(sim, market, spec);
    sim.initialize();

    addOrder(SELL, 90);
    addOrder(BUY, 50);
    addTransaction(60);
    addTransaction(70);
    addTransaction(80);

    assertQuote(view.getQuote(), 50, 120);

    addTransaction(80);
    addTransaction(70);

    // 120 is gone because trend agent withdraws it's old orders when targeting a new trend
    assertQuote(view.getQuote(), 20, ABSENT);
  }

  private void advance() {
    sim.executeUntil(TimeStamp.of(sim.getCurrentTime().get() + 1));
  }

  private void addOrder(OrderType buyOrSell, long ticksPrice) {
    view.submitOrder(buyOrSell, Price.of(ticksPrice), 1);
    advance();
  }

  private void addTransaction(long ticksPrice) {
    Price price = Price.of(ticksPrice);
    view.submitOrder(BUY, price, 1);
    view.submitOrder(SELL, price, 1);
    advance();
  }

}
