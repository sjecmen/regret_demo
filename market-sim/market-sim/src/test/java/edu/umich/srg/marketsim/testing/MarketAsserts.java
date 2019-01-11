package edu.umich.srg.marketsim.testing;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Optional;

import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.market.Quote;

public interface MarketAsserts {

  /** A sentinel value that can be used for absent quotes when just using long prices */
  static long ABSENT = Long.MIN_VALUE;

  /** Prices are null for absent prices for convenience */
  static void assertQuote(Quote quote, Price bid, Price ask) {
    assertEquals("Incorrect bid", Optional.fromNullable(bid), quote.getBidPrice());
    assertEquals("Incorrect ask", Optional.fromNullable(ask), quote.getAskPrice());
  }

  /** Use the ABSENT sentinel for missing bids or asks */
  static void assertQuote(Quote quote, long bid, long ask) {
    assertQuote(quote, bid == ABSENT ? null : Price.of(bid), ask == ABSENT ? null : Price.of(ask));
  }

  static void assertQuote(Quote quote, Price bid, int bidDepth, Price ask, int askDepth) {
    assertEquals("Incorrect bid", Optional.fromNullable(bid), quote.getBidPrice());
    assertEquals("Incorrect ask", Optional.fromNullable(ask), quote.getAskPrice());
    assertEquals("Incorrect bid depth", bidDepth, quote.getBidDepth());
    assertEquals("Incorrect ask depth", askDepth, quote.getAskDepth());
  }

  static void assertQuote(Quote quote, long bid, int bidDepth, long ask, int askDepth) {
    assertQuote(quote, bid == ABSENT ? null : Price.of(bid), bidDepth,
        ask == ABSENT ? null : Price.of(ask), askDepth);
  }

}
