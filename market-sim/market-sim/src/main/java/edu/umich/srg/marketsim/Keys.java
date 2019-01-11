package edu.umich.srg.marketsim;

import com.google.common.base.Converter;

import edu.umich.srg.egtaonline.spec.ParsableValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.BoolValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.DoubleValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.EnumValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.IntValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.LongValue;
import edu.umich.srg.egtaonline.spec.ParsableValue.StringsValue;
import edu.umich.srg.egtaonline.spec.Spec;
import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.agent.StandardMarketAgent.OrderStyle;

/**
 * This file contains all of the type safe keys used in Spec object for initializing agents and
 * markets appropriately. These are initially read from strings in a simulation spec file, snd
 * therefore all of the keys must be in this file with public classes with public constructors so
 * that these classes can be properly instantiated. At the bottom of this file is a default spec
 * object. This is used to assign defaults for any parameters. In general this should be as sparse
 * as possible, but some keys have obvious defaults.
 */
public interface Keys {
  // ----------
  // Simulation
  // ----------

  /** The seed used to initialize all observations. If left out, it is initialized with the time. */
  class RandomSeed extends LongValue {
  }

  /** The number of time steps in the simulator. */
  class SimLength extends LongValue {
  }

  /** The markets that are constructed in the simulator. */
  class Markets extends StringsValue {
  }

  /** The mean of the gaussain fundamental. */
  class FundamentalMean extends DoubleValue {
  }

  /** The variance of the shocks in the fundamental. */
  class FundamentalShockVar extends DoubleValue {
  }

  /** The rate at which the fundamental reverts towards the mean. */
  class FundamentalMeanReversion extends DoubleValue {
  }

  /** The probability of a jump. */
  class FundamentalShockProb extends DoubleValue {
  }

  // -------
  // Markets
  // -------

  /** Whether the call market biases towards buyer or seller, 0 -> buyer price 1 -> seller price. */
  class Pricing extends DoubleValue {
  }

  /** The clearing interval for a call market. */
  class ClearInterval extends LongValue {
  }

  // ------
  // Agents
  // ------

  /** The probability that an agent arrives at any time step. */
  class ArrivalRate extends DoubleValue {
  }

  /** Variance of added noise to any fundamental observation. */
  class FundamentalObservationVariance extends DoubleValue {
  }

  /** Whether an agent should buy or sell. */
  class Type extends EnumValue<OrderType> {
    public Type() {
      super(OrderType.class);
    }
  }

  /** The maximum absolute position an agent can hold. */
  class MaxPosition extends IntValue {
  }

  /** Variance used in generating an agents private value. */
  class PrivateValueVar extends DoubleValue {
  }

  /** The maximum surplus an agent will demand. */
  class Rmax extends IntValue {
  }

  /** The minimum surplus an agent will demand. */
  class Rmin extends IntValue {
  }

  /** How agents should decide what side to pick. */
  class Sides extends EnumValue<OrderStyle> {
    public Sides() {
      super(OrderStyle.class);
    }
  }

  /** How many orders per side an agent should submit. */
  class SubmitDepth extends IntValue {
  }

  // -------------------------
  // Specific agent parameters
  // -------------------------

  /** Markov agent's estimate variance for price relative to the fundamental. */
  class PriceVarEst extends DoubleValue {
  }

  /** Number of order the shock agent will submit. */
  class NumShockOrders extends IntValue {
  }

  /** The fraction of demanded surplus necessary for an agent to submit an order at bid or ask. */
  class Thresh extends DoubleValue {
  }

  /** Amount of time the shock agent has to liquidate. */
  class TimeToLiquidate extends LongValue {
  }

  // ------------
  // GDV Agent
  // ------------
  public class NumTransactions extends IntValue {
  }

  public class RecencyDiscount extends DoubleValue {
  }

  // ------------
  // spoofing Agent
  // ------------
  public class SpoofUnits extends IntValue {
  }

  public class SpoofingTime extends LongValue {
  }

  public class SpoofingTimeOne extends LongValue {
  }

  public class SpoofingTimeTwo extends LongValue {
  }

  public class ProfitTimeOne extends LongValue {
  }

  public class ProfitTimeTwo extends LongValue {
  }

  public class WaitingInterval extends LongValue {
  }

  public class IncrementalInterval extends LongValue {
  }

  public class Threshold extends DoubleValue {
  }

  // ------------
  // Market Maker
  // ------------
  /** Separation between successive rungs. */
  class RungSep extends IntValue {
  }

  /** Number of rungs to submit. */
  class NumRungs extends IntValue {
  }

  /** Whether to tweak rung by a tick. */
  class TickImprovement extends BoolValue {
  }

  /** Place tick improvement outside spread, instead of default inside. */
  class TickOutside extends BoolValue {
  }

  /** Number of orders per rung. */
  class RungThickness extends IntValue {
  }

  /** Spread between best bid and best ask. */
  class Spread extends DoubleValue {
  }

  // -------------------
  // Trend Following HFT
  // -------------------

  /** The minimum length of a monotonic trend to act. */
  class TrendLength extends IntValue {
  }

  /** The maximum profit demanded from a trend front run. */
  class ProfitDemanded extends IntValue {
  }

  /** The max amount of time to leave an order in the market. */
  class Expiration extends TimeValue {
  }
  /* Old leftover keys. */

  // public static class DiscountFactors extends DoublesValue {};
  //
  // public static class FileName extends StringValue {};
  //
  // public static class Window extends TimeValue {};
  // public static class Alpha extends DoubleValue {};
  //
  //
  //
  // public static class Num extends IntValue {};
  // public static class NumAgents extends IntValue {};
  // public static class NumMarkets extends IntValue {};

  // Latency
  // public static class NbboLatency extends TimeValue {};
  // public static class MarketLatency extends TimeValue {};
  // public static class LaLatency extends TimeValue {};
  // public static class FundamentalLatency extends TimeValue {};

  // Call Market
  // public static class ClearInterval extends TimeValue {};
  // public static class PricingPolicy extends DoubleValue {};

  // Agents
  // public static class Withdraw extends BoolValue {};
  // public static class N extends IntValue {};

  // public static class ReentryType extends EnumValue<Reentries> { public ReentryType() {
  // super(Reentries.class); }};

  // Market Maker
  // public static class K extends IntValue {};
  // public static class Size extends IntValue {};
  // public static class Trunc extends BoolValue {};
  // public static class InitLadderMean extends IntValue {};
  // public static class InitLadderRange extends IntValue {};

  // AAAgent
  // public static class Eta extends IntValue {};
  // public static class LambdaR extends DoubleValue {};
  // public static class LambdaA extends DoubleValue {};
  // public static class Gamma extends DoubleValue {};
  // public static class BetaR extends DoubleValue {};
  // public static class BetaT extends DoubleValue {};
  // public static class InitAggression extends DoubleValue {};
  // public static class Theta extends DoubleValue {};
  // public static class ThetaMax extends DoubleValue {};
  // public static class ThetaMin extends DoubleValue {};
  // public static class Debug extends BoolValue {};
  // public static class BuyerStatus extends EnumValue<OrderType> { public BuyerStatus() {
  // super(OrderType.class); }};

  // ZIPAgent
  // public static class MarginMin extends DoubleValue {};
  // public static class MarginMax extends DoubleValue {};
  // public static class GammaMin extends DoubleValue {};
  // public static class GammaMax extends DoubleValue {};
  // public static class BetaMin extends DoubleValue {};
  // public static class BetaMax extends DoubleValue {};
  // public static class RangeR extends DoubleValue {};
  // public static class RangeA extends DoubleValue {};

  // Market Makers
  // public static class W extends DoubleValue {};
  // public static class Strats extends IntsValue {};
  // public static class UseMedianSpread extends BoolValue {};
  // public static class MovingAveragePrice extends BoolValue {};
  // public static class FastLearning extends BoolValue {};
  // public static class UseLastPrice extends BoolValue {};
  // public static class FundEstimate extends PriceValue {};
  // public static class Spread extends PriceValue {};

  // ZIRPAgent


  // --------------
  // Helper Classes
  // --------------

  /** Spec parameter that's a TimeStamp. */
  class TimeValue extends ParsableValue<TimeStamp> {
    protected TimeValue() {
      super(new Converter<String, TimeStamp>() {
        @Override
        protected String doBackward(TimeStamp time) {
          return Long.toString(time.get());
        }

        @Override
        protected TimeStamp doForward(String string) {
          return TimeStamp.of(Long.parseLong(string));
        }
      });
    }
  }

  // static class PriceValue extends ParsableValue<Price> {
  // protected PriceValue() {
  // super(new Converter<String, Price>() {
  // @Override protected String doBackward(Price price) { return Long.toString(price.longValue()); }
  // @Override protected Price doForward(String string) { return Price.of(Long.parseLong(string)); }
  // });
  // }
  // }

  /**
   * The simulation will use these as the defaults for any unspecified keys. This should be as
   * sparse as possible, and only used when one shouldn't be forced to manually specify something.
   */
  Spec DEFAULT_KEYS = Spec.builder() //
      .put(RandomSeed.class, System.nanoTime()) // Set seed from clock
      .put(FundamentalMean.class, 1e9) // Approximately half of Integer.MAX_VALUE
      .put(FundamentalShockProb.class, 1d) // Shocks disabled

      .put(Pricing.class, 0.5) // Even call market

      .put(Sides.class, OrderStyle.RANDOM) // Submit orders randomly (legacy)
      .put(SubmitDepth.class, 1) // Submit one order per arrival (legacy)
      .put(Thresh.class, 1d) // No threshold
      .build();
}

