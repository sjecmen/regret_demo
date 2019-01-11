package edu.umich.srg.marketsim.strategy;

import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.FLOOR;

import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

import edu.umich.srg.fourheap.Order.OrderType;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

import java.util.HashMap;
import java.util.Map;

public class SurplusThreshold {

  // This class is memoized on the assumption that not many different thresholds will be used
  // TODO consider moving to guava LoadingCache
  private static final Map<Double, SurplusThreshold> memoized = new HashMap<>();

  private final double threshold;

  private SurplusThreshold(double threshold) {
    this.threshold = threshold;
  }

  public static SurplusThreshold create(double threshold) {
    return memoized.computeIfAbsent(threshold, SurplusThreshold::new);
  }

  /** Get the price to submit conditioned on the demanded surplus and the market state. */
  public Price shadePrice(OrderType type, Quote quote, double estimatedValue,
      double demandedSurplus) {
    Optional<Price> marketPrice = type == BUY ? quote.getAskPrice() : quote.getBidPrice();

    // Strategic shading for guaranteed surplus
    if (marketPrice.isPresent() && type.sign()
        * (estimatedValue - marketPrice.get().doubleValue()) > this.threshold * demandedSurplus) {

      return marketPrice.get();

    } else { // Submit order that won't transact immediately
      // Round beneficially
      long roundedPrice = DoubleMath.roundToLong(estimatedValue - type.sign() * demandedSurplus,
          type == BUY ? FLOOR : CEILING);

      return Price.of(roundedPrice);
    }
  }

}
