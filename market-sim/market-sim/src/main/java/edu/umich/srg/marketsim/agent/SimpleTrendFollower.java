package edu.umich.srg.marketsim.agent;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.Expiration;
import edu.umich.srg.marketsim.Keys.ProfitDemanded;
import edu.umich.srg.marketsim.Keys.TrendLength;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;
import edu.umich.srg.marketsim.market.OrderRecord;
import edu.umich.srg.marketsim.market.Quote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.function.Function;

/**
 * This agent looks for short monotonic trends that may indicate a price jump, and tries to exploit
 * them. Only increases or decreases in price cause an increase in the length of the trend. Repeated
 * transactions at the same price do not indicate a long trend.
 */
/*
 * Not counting equal prices is an easy way to ignore having a long chain of equals prices count as
 * either an increasing or decreasing trend.
 * 
 * TODO Decide if agent should withdraw order when the trend changes direction. This seems like a
 * yes?
 * 
 * TODO Enforce position limits, and decided how this should be done. a) stop trading in one
 * direction b) submit market orders to slowly keep position close to zero.
 */
public class SimpleTrendFollower implements Agent {

  private final Sim sim;
  private final MarketView market;
  private final int trendLength;
  private final int profitDemanded;
  private final TimeStamp expiration;

  private boolean ignoreNext;
  private int currentLength;
  private OrderType direction;
  private Price lastPrice;

  /** Basic constructor of a simple trend follower. */
  public SimpleTrendFollower(Sim sim, Market market, Spec spec) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.trendLength = spec.get(TrendLength.class);
    this.profitDemanded = spec.get(ProfitDemanded.class);
    this.expiration = spec.get(Expiration.class);

    this.ignoreNext = false;
    this.currentLength = 0;
    this.direction = null;
    this.lastPrice = null;
  }

  public static SimpleTrendFollower createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new SimpleTrendFollower(sim, market, spec);
  }

  private void strategy() {
    ImmutableList.copyOf(market.getActiveOrders()).forEach(market::withdrawOrder);
    Function<Quote, Optional<Price>> getQuotePrice =
        direction == BUY ? Quote::getAskPrice : Quote::getBidPrice;
    Quote quote = market.getQuote();

    if (getQuotePrice.apply(quote).isPresent()) {
      Price old = getQuotePrice.apply(quote).get();
      // Take best order
      ignoreNext = true;
      market.submitOrder(direction, old, 1);
      // Make find next highest order on that side
      quote = market.getQuote();
      long nextPrice = getQuotePrice.apply(quote).transform(p -> p.longValue() * direction.sign())
          .or(Long.MAX_VALUE);
      // Find the limiting price so that we're still front running, make sure profit is positive
      long limit = Math.max(direction.sign() * old.longValue(), nextPrice - 1);
      // Go a step up from the previous price, but keep under limit
      Price stepped = Price.of(
          direction.sign() * Math.min(direction.sign() * old.longValue() + profitDemanded, limit));

      OrderRecord record = market.submitOrder(direction.opposite(), stepped, 1);
      sim.scheduleIn(expiration, () -> market.withdrawOrder(record));
    }
  }

  @Override
  public void initialize() {}

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public String toString() {
    return "STF " + Integer.toString(System.identityHashCode(this), 36).toUpperCase();
  }

  @Override
  public void notifyTransaction(MarketView market, OrderNotification notification) {
    Price price = notification.getPrice();
    if (direction == null) {
      direction = BUY;
      currentLength = 1;
    } else if (!price.equals(lastPrice) && (price.compareTo(lastPrice) < 0) ^ (direction == BUY)) {
      currentLength++;
    } else if (!price.equals(lastPrice)) {
      currentLength = 2;
      direction = direction.opposite();
    }
    lastPrice = price;
    if (ignoreNext) {
      ignoreNext = false;
    } else if (currentLength >= trendLength) {
      sim.scheduleIn(TimeStamp.ZERO, this::strategy);
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

  @Override
  public ArrayList<Double> getBeliefBias() {
    return new ArrayList<>();
  }

  @Override
  public ArrayList<Double> getSubmissionDist() {
    return new ArrayList<>();
  }

}
