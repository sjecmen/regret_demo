package edu.umich.srg.marketsim.agent;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;

import com.google.common.math.DoubleMath;

import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Keys.NumShockOrders;
import edu.umich.srg.marketsim.Keys.SimLength;
import edu.umich.srg.marketsim.Keys.TimeToLiquidate;
import edu.umich.srg.marketsim.Keys.Type;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.Sim;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.marketsim.fundamental.Fundamental;
import edu.umich.srg.marketsim.market.Market;
import edu.umich.srg.marketsim.market.Market.MarketView;
import edu.umich.srg.marketsim.market.OrderNotification;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.function.IntSupplier;

/**
 * ShockAgent is an agent that liquidates a large quantity to buy or sell in a short period of time.
 */
public class ShockAgent implements Agent {

  private final Sim sim;
  private final MarketView market;
  private final OrderType type;
  private final Price orderPrice;
  private final IntSupplier getDepth;

  private final long arrivalTime;
  private final long timeToLiquidate;
  private final long totalOrdersToSubmit;

  private int ordersToSubmit;
  private boolean waitingToSubmit;

  /** Standard constructor. */
  public ShockAgent(Sim sim, Market market, Spec spec) {
    this.sim = sim;
    this.market = market.getView(this, TimeStamp.ZERO);
    this.arrivalTime = spec.get(SimLength.class) / 2;
    this.timeToLiquidate = spec.get(TimeToLiquidate.class);
    this.type = spec.get(Type.class);
    this.orderPrice = type == BUY ? Price.INF : Price.ZERO;
    this.totalOrdersToSubmit = this.ordersToSubmit = spec.get(NumShockOrders.class);
    this.waitingToSubmit = false;

    this.getDepth = type == BUY ? () -> this.market.getQuote().getAskDepth()
        : () -> this.market.getQuote().getBidDepth();
  }

  public static ShockAgent createFromSpec(Sim sim, Fundamental fundamental,
      Collection<Market> markets, Market market, Spec spec, Random rand) {
    return new ShockAgent(sim, market, spec);
  }

  private void strategy() {
    int depth = this.getDepth.getAsInt();
    if (ordersToSubmit > 0 && depth > 0) {
      int submitNow = ordersToSubmit
          - DoubleMath.roundToInt((arrivalTime + timeToLiquidate - sim.getCurrentTime().get())
              / (double) timeToLiquidate * totalOrdersToSubmit, RoundingMode.HALF_EVEN);
      int quantity = Math.min(ordersToSubmit, Math.min(submitNow, depth));

      if (quantity > 0) {
        market.submitOrder(type, orderPrice, quantity);
        ordersToSubmit -= quantity;
      }

      waitingToSubmit = false;
      sim.scheduleIn(TimeStamp.of(1), this::strategy);

    } else if (ordersToSubmit > 0) {
      waitingToSubmit = true;
    }
  }

  @Override
  public void initialize() {
    sim.scheduleIn(TimeStamp.of(arrivalTime), this::strategy);
  }

  @Override
  public double payoffForPosition(int position) {
    return 0;
  }

  @Override
  public void notifyQuoteUpdated(MarketView market) {
    if (waitingToSubmit) {
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
  public void notifyTransaction(MarketView market, OrderNotification notification) {
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
