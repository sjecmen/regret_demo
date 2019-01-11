package edu.umich.srg.marketsim.market;

import com.google.common.base.Optional;

import edu.umich.srg.marketsim.Price;

import java.io.Serializable;
import java.util.Objects;

/** Container for Quote data. */
public class Quote implements Serializable {

  private static final Quote empty = new Quote(null, 0, null, 0);

  private final Optional<Price> ask;
  private final Optional<Price> bid;
  private final int bidDepth;
  private final int askDepth;

  Quote(Price bid, int bidDepth, Price ask, int askDepth) {
    this.ask = Optional.fromNullable(ask);
    this.bid = Optional.fromNullable(bid);
    this.bidDepth = bidDepth;
    this.askDepth = askDepth;
  }

  static Quote empty() {
    return empty;
  }

  public Optional<Price> getAskPrice() {
    return ask;
  }

  public Optional<Price> getBidPrice() {
    return bid;
  }

  public int getBidDepth() {
    return bidDepth;
  }

  public int getAskDepth() {
    return askDepth;
  }

  /** True if the quote is defined (has an ask and a bid price). */
  public boolean isDefined() {
    return ask.isPresent() && bid.isPresent();
  }

  /** bid-ask spread of the quote. */
  public double getSpread() {
    // FIXME Are these the best way to handle these cases?
    if (!ask.isPresent() || !bid.isPresent()) {
      return Double.POSITIVE_INFINITY;
    } else {
      return ask.get().doubleValue() - bid.get().doubleValue();
    }
  }

  /** Return the midquote. */
  public double getMidquote() {
    // FIXME Are these the best way to handle these cases?
    if (!ask.isPresent() || !bid.isPresent()) {
      return Double.NaN;
    } else {
      return (ask.get().doubleValue() + bid.get().doubleValue()) / 2;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(ask, bid);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Quote)) {
      return false;
    } else {
      Quote that = (Quote) other;
      return Objects.equals(ask, that.ask) && Objects.equals(bid, that.bid);
    }
  }

  @Override
  public String toString() {
    return "(Bid: " + (bid.isPresent() ? bid.get() : "- ") + ", Ask: "
        + (ask.isPresent() ? ask.get() : "- ") + ')';
  }

  private static final long serialVersionUID = 1;

}
