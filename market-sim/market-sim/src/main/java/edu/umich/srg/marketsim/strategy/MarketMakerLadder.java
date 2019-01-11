package edu.umich.srg.marketsim.strategy;

import static com.google.common.base.Preconditions.checkArgument;
import static edu.umich.srg.fourheap.Order.OrderType.BUY;
import static edu.umich.srg.fourheap.Order.OrderType.SELL;

import edu.umich.srg.marketsim.Price;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class MarketMakerLadder {

  private final int stepSize;
  private final int numRungs;
  private final int offset;

  /** Constructor of a market maker ladder. */
  public MarketMakerLadder(int rungSeperation, int numRungs, boolean tickImprovement,
      boolean tickOutside) {
    checkArgument(rungSeperation > 0);
    checkArgument(numRungs > 0);
    this.stepSize = rungSeperation;
    this.numRungs = numRungs;
    this.offset = tickImprovement ? (tickOutside ? 1 : -1) : 0;
  }

  /** Creates an order ladder as a stream of order descriptions. */
  public Stream<OrderDesc> createLadder(Price highestBuy, Price lowestSell) {
    Stream<OrderDesc> buys = toStream(-highestBuy.longValue(), lowestSell.longValue()).map(p -> -p)
        .mapToObj(p -> OrderDesc.of(BUY, Price.of(p)));
    Stream<OrderDesc> sells = toStream(lowestSell.longValue(), -highestBuy.longValue())
        .mapToObj(p -> OrderDesc.of(SELL, Price.of(p)));
    return Stream.concat(sells, buys);
  }

  /** Creates a stream of order prices. */
  private LongStream toStream(long init, long cross) {
    // XXX change filter to takeWhile when it exists
    return LongStream.range(0, numRungs).map(s -> init + offset + stepSize * s)
        .filter(p -> p > -(cross + offset));
  }

}
