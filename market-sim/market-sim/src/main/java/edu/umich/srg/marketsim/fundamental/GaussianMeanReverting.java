package edu.umich.srg.marketsim.fundamental;

import com.google.common.primitives.Ints;

import edu.umich.srg.collect.Sparse;
import edu.umich.srg.distributions.Binomial;
import edu.umich.srg.distributions.Gaussian;
import edu.umich.srg.distributions.Hypergeometric;
import edu.umich.srg.marketsim.Price;
import edu.umich.srg.marketsim.TimeStamp;
import edu.umich.srg.util.PositionalSeed;
import edu.umich.srg.util.SummStats;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

/**
 * This class models a Guassian mean reverting process that doesn't necessarily make a jump at every
 * time step. It has two important implementation features. First, fundamental values are computed
 * lazily and on demand, so that even if you ask for the fundamental value at time 10000000, it
 * should return reasonable fast, without having to generate all 1000000 values. It is also randomly
 * stable, that is, two fundamentals with the same random generator will produce the same value at
 * every point independent of query order. This costs a logarithmic factor to do, but the stability
 * is generally worth it, and the log factor is tiny in terms of actual time costs. More detail on
 * the math for sampling from the fundamental is in the docs folder.
 */

/*
 * FIXME have IID Gaussians only track "if there was a jump", not "how many jumps" to make sampling
 * more efficient
 */

/*
 * There are two major design decisions that will help understand how this works. The first is that
 * to avoid constantly checking for special cases, these are broken into subclasses, and since the
 * cases are two dimensional (one for how to process the jumps, and one for when they occur) there
 * is a class nesting. This is where the Sampler interface comes in.
 * 
 * The second is that to keep the draws consistent independent of order, we do some extra sampling.
 * The extra sampling comes in the form of binary search. With existing points, if the time 15 is
 * queried, this will first sample points at 1, 2, 4, 8, 16, 12, 14, 15. This may seems like a lot,
 * but it's generally log in the distance. The second piece necessary is to condition the sameple on
 * the time when the occur. That way, the random generator at time t is always the same, and always
 * conditioned on the same draws, thus given consistent draws independent of order. To facilitate
 * the final part, the positional seed class is used which generates more uniform seeds for
 * sequential values, making sure that this is close uniform pseudo random.
 * 
 * Calculating the equations for the rmsd were non trivial and were done using mathematica's
 * symbolic toolbox. See the resources directory for the notebooks that calculated them.
 */

public abstract class GaussianMeanReverting implements Fundamental, Serializable {

  /** Create a standard gaussian mean reverting fundamental stochastic process. */
  public static GaussianMeanReverting create(Random rand, double mean, double meanReversion,
      double shockVar, double shockProb) {
    if (shockProb == 0) {
      return ConstantFundamental.create(Price.of(mean));
    } else {
      Sampler sampler;
      if (meanReversion == 0) {
        sampler = new RandomWalk(rand, shockVar);
      } else if (meanReversion == 1) {
        sampler = new IidGaussian(rand, mean, shockVar);
      } else {
        sampler = new MeanReverting(rand, mean, shockVar, meanReversion);
      }
      if (shockProb == 1) {
        return new JumpEvery(mean, sampler);
      } else {
        return new JumpRandomlyCount(mean, sampler, shockProb, rand);
      }
    }
  }

  private abstract static class AbstractGaussianMeanReverting<F extends FundamentalObservation>
      extends GaussianMeanReverting {

    private final NavigableMap<Long, F> fundamental;

    private AbstractGaussianMeanReverting(F initial) {
      // Put in zero and one, so doubling works
      this.fundamental = new TreeMap<>();
      fundamental.put(0L, initial);
    }

    @Override
    public Price getValueAt(TimeStamp timeStamp) {
      long time = timeStamp.get();

      // First make sure that time is in the map by binary searching up
      Entry<Long, F> last = fundamental.lastEntry();
      while (time > last.getKey()) {
        long nextTime = last.getKey() == 0 ? 1 : last.getKey() * 2;
        F observation = observeFuture(last, nextTime);
        fundamental.put(nextTime, observation);
        last = new AbstractMap.SimpleImmutableEntry<>(nextTime, observation);
      }

      Entry<Long, F> before = fundamental.floorEntry(time);
      Entry<Long, F> after = fundamental.ceilingEntry(time);

      while (before.getKey() != time && after.getKey() != time) {
        long midTime = (before.getKey() + after.getKey()) / 2;
        F observation = observeIntermediate(before, after, midTime);
        fundamental.put(midTime, observation);
        Entry<Long, F> entry = new AbstractMap.SimpleImmutableEntry<>(midTime, observation);
        if (midTime > time) {
          after = entry;
        } else {
          before = entry;
        }
      }

      if (before.getKey() == time) {
        return Price.of(before.getValue().price).nonnegative();
      } else {
        return Price.of(after.getValue().price).nonnegative();
      }
    }

    @Override
    public Iterable<Sparse.Entry<Number>> getFundamentalValues(TimeStamp finalTime) {
      // TODO Replace filter with takeWhile
      long longTime = finalTime.get();
      return () -> fundamental.entrySet().stream().filter(e -> e.getKey() <= longTime)
          .map(e -> Sparse.<Number>immutableEntry(e.getKey(), Price.of(e.getValue().doubleValue())))
          .iterator();
    }

    protected Iterable<Entry<Long, F>> getEntriesIterable() {
      return fundamental.entrySet();
    }

    protected abstract F observeFuture(Entry<Long, F> last, long time);

    protected abstract F observeIntermediate(Entry<Long, F> before, Entry<Long, F> after,
        long time);

    private static final long serialVersionUID = 1;

  }

  private static class JumpEvery extends AbstractGaussianMeanReverting<FundamentalObservation> {

    private final Sampler sampler;

    private JumpEvery(double mean, Sampler sampler) {
      super(new FundamentalObservation(mean));
      this.sampler = sampler;
    }

    @Override
    protected FundamentalObservation observeFuture(Entry<Long, FundamentalObservation> last,
        long time) {
      double price = sampler.getFutureValue(time, last.getValue().price, time - last.getKey());
      return new FundamentalObservation(price);
    }

    @Override
    protected FundamentalObservation observeIntermediate(Entry<Long, FundamentalObservation> before,
        Entry<Long, FundamentalObservation> after, long time) {
      double newPrice = sampler.getIntermediateValue(time, before.getValue().price,
          time - before.getKey(), after.getValue().price, after.getKey() - time);
      return new FundamentalObservation(newPrice);
    }

    @Override
    public double rmsd(Iterator<? extends Sparse.Entry<? extends Number>> prices,
        TimeStamp finalTime) {
      if (!prices.hasNext()) {
        return Double.NaN;
      }
      getValueAt(finalTime); // Make sure there's a final time fundamental node

      Iterator<? extends Entry<Long, ? extends Number>> fundamental =
          getEntriesIterable().iterator();

      SummStats rmsd = SummStats.empty();
      Entry<Long, ? extends Number> lastFundamental = fundamental.next();
      Sparse.Entry<? extends Number> lastPrice = prices.next();

      while (lastFundamental.getKey().compareTo(lastPrice.getIndex()) < 0) {
        lastFundamental = fundamental.next();
      }

      if (!prices.hasNext()) {
        double finalEstimate = sampler.getFinalEstimate(lastFundamental.getKey(),
            lastFundamental.getValue().doubleValue(), finalTime.get());
        double diff = finalEstimate - lastPrice.getElement().doubleValue();
        rmsd.accept(diff * diff);
      } else {
        while (prices.hasNext()) {
          Sparse.Entry<? extends Number> nextPrice = prices.next();
          while (lastFundamental.getKey().compareTo(nextPrice.getIndex()) < 0) {
            double finalEstimate = sampler.getFinalEstimate(lastFundamental.getKey(),
                lastFundamental.getValue().doubleValue(), finalTime.get());
            double diff = finalEstimate - lastPrice.getElement().doubleValue();
            rmsd.accept(diff * diff);
            lastFundamental = fundamental.next();
          }
          lastPrice = nextPrice;
        }
        while (lastFundamental.getKey().compareTo(finalTime.get()) < 0) {
          double finalEstimate = sampler.getFinalEstimate(lastFundamental.getKey(),
              lastFundamental.getValue().doubleValue(), finalTime.get());
          double diff = finalEstimate - lastPrice.getElement().doubleValue();
          rmsd.accept(diff * diff);
          lastFundamental = fundamental.next();
        }
        double finalEstimate = sampler.getFinalEstimate(lastFundamental.getKey(),
            lastFundamental.getValue().doubleValue(), finalTime.get());
        double diff = finalEstimate - lastPrice.getElement().doubleValue();
        rmsd.accept(diff * diff);
      }
      return Math.sqrt(rmsd.getAverage());
    }

    private static final long serialVersionUID = 1;

  }

  private static class JumpRandomlyCount
      extends AbstractGaussianMeanReverting<JumpFundamentalObservation> {

    private final PositionalSeed seed;
    private final Random rand;
    private final Sampler sampler;
    private final double shockProb;

    private JumpRandomlyCount(double mean, Sampler sampler, double shockProb, Random rand) {
      super(new JumpFundamentalObservation(mean, 0));
      this.shockProb = shockProb;
      this.sampler = sampler;
      this.seed = PositionalSeed.with(rand.nextLong());
      this.rand = rand;
    }

    @Override
    protected JumpFundamentalObservation observeFuture(Entry<Long, JumpFundamentalObservation> last,
        long time) {
      rand.setSeed(seed.getSeed(time));
      long jumps = Binomial.with(time - last.getKey(), shockProb).sample(rand);
      double price = jumps == 0 ? last.getValue().price
          : sampler.getFutureValue(time, last.getValue().price, jumps);
      return new JumpFundamentalObservation(price, jumps);
    }

    @Override
    protected JumpFundamentalObservation observeIntermediate(
        Entry<Long, JumpFundamentalObservation> before,
        Entry<Long, JumpFundamentalObservation> after, long time) {
      rand.setSeed(seed.getSeed(time));
      int jumpsBefore = Hypergeometric.with(Ints.checkedCast(after.getKey() - before.getKey()),
          Ints.checkedCast(after.getValue().jumpsBefore), Ints.checkedCast(time - before.getKey()))
          .sample(rand);
      after.getValue().jumpsBefore -= jumpsBefore;

      double newPrice;
      if (jumpsBefore == 0) {
        newPrice = before.getValue().price;
      } else if (after.getValue().jumpsBefore == 0) {
        newPrice = after.getValue().price;
      } else {
        newPrice = sampler.getIntermediateValue(time, before.getValue().price, jumpsBefore,
            after.getValue().price, after.getValue().jumpsBefore);
      }

      return new JumpFundamentalObservation(newPrice, jumpsBefore);
    }

    @Override
    public double rmsd(Iterator<? extends Sparse.Entry<? extends Number>> prices,
        TimeStamp finalTime) {
      // FIXME This is unimplemented, and doesn't seem super trivial to accomplish
      return Double.NaN;
    }

    private static final long serialVersionUID = 1;

  }

  private interface Sampler {

    double getFinalEstimate(long currentTime, double currentFundamental, long finalTime);

    double getFutureValue(long time, double lastPrice, long jumps);

    double getIntermediateValue(long time, double priceBefore, long jumpsBefore, double priceAfter,
        long jumpsAfter);

    /**
     * Returns the expected average rmsd between a and b inclusive where price was `price`, the
     * fundamental at time zero was fundamentalZero, and fundamentalEnd at end.
     */
    double expectedAverageIntermediateRmsd(double fundamentalZero, double fundamentalEnd,
        double price, long timeA, long timeB, long end);

  }

  private static class RandomWalk implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;

    private RandomWalk(Random rand, double shockVar) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.shockVar = shockVar;
      this.rand = rand;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      return Gaussian.withMeanVariance(lastPrice, shockVar * jumps).sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      return Gaussian
          .withMeanVariance(
              (priceBefore * jumpsAfter + priceAfter * jumpsBefore) / (jumpsBefore + jumpsAfter),
              jumpsBefore * jumpsAfter / (double) (jumpsBefore + jumpsAfter) * shockVar)
          .sample(rand);
    }

    // Calculated using mathematica
    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalZero, double fundamentalEnd,
        double price, long timeA, long timeB, long end) {
      double temp1 = -fundamentalZero;
      double temp2 = fundamentalEnd + temp1;
      double temp3 = 6 * end * fundamentalZero;
      double temp4 = -6 * end * price;
      double temp5 = 3 * end;
      double temp6 = Math.pow(temp2, 2) - (end * shockVar);
      return (6 * Math.pow(end, 2) * Math.pow(fundamentalZero - price, 2)
          + timeB * temp2 * (fundamentalEnd + temp1 + temp3 + temp4)
          + timeA * temp2
              * ((-1 + 2 * timeB) * fundamentalEnd + fundamentalZero - 2 * timeB * fundamentalZero
                  + temp3 + temp4)
          + timeB * end * (-1 + temp5) * shockVar + timeA * end * (1 - 2 * timeB + temp5) * shockVar
          + 2 * Math.pow(timeA, 2) * temp6 + 2 * Math.pow(timeB, 2) * temp6)
          / (6. * Math.pow(end, 2));
    }

    private static final long serialVersionUID = 1;

    @Override
    public double getFinalEstimate(long currentTime, double currentFundamental, long finalTime) {
      return currentFundamental;
    }

  }

  private static class IidGaussian implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final Gaussian dist;

    private IidGaussian(Random rand, double mean, double shockVar) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.dist = Gaussian.withMeanVariance(mean, shockVar);
      this.rand = rand;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      return dist.sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      return dist.sample(rand);
    }

    /* Average RMSD is always the same conditioned on price. */
    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalZero, double fundamentalEnd,
        double price, long timeA, long timeB, long end) {
      return dist.getVariance() + dist.getMean() * dist.getMean() - 2 * dist.getMean() * price
          + price * price;
    }

    private static final long serialVersionUID = 1;

    @Override
    public double getFinalEstimate(long currentTime, double currentFundamental, long finalTime) {
      // TODO Auto-generated method stub
      return 0;
    }

  }

  private static class MeanReverting implements Sampler, Serializable {

    private final PositionalSeed seed;
    private final Random rand;
    private final double shockVar;
    private final double mean;
    private final double kappac;

    private MeanReverting(Random rand, double mean, double shockVar, double meanReversion) {
      this.seed = PositionalSeed.with(rand.nextLong());
      this.mean = mean;
      this.shockVar = shockVar;
      this.kappac = 1 - meanReversion;
      this.rand = rand;
    }

    public double getFinalEstimate(long currentTime, double currentFundamental, long finalTime) {
      double kappacToPower = Math.pow(kappac, finalTime - currentTime);
      return (1 - kappacToPower) * mean + kappacToPower * currentFundamental;
    }

    @Override
    public double getFutureValue(long time, double lastPrice, long jumps) {
      rand.setSeed(seed.getSeed(time));
      double kappacToPower = Math.pow(kappac, jumps);
      double stepMean = (1 - kappacToPower) * mean + kappacToPower * lastPrice;
      double stepVar = (1 - kappacToPower * kappacToPower) / (1 - kappac * kappac);
      return Gaussian.withMeanVariance(stepMean, shockVar * stepVar).sample(rand);
    }

    @Override
    public double getIntermediateValue(long time, double priceBefore, long jumpsBefore,
        double priceAfter, long jumpsAfter) {
      rand.setSeed(seed.getSeed(time));
      double kappacPowerBefore = Math.pow(kappac, jumpsBefore);
      double kappacPowerAfter = Math.pow(kappac, jumpsAfter);
      double stepMean = ((kappacPowerBefore - 1) * (kappacPowerAfter - 1)
          * (kappacPowerBefore * kappacPowerAfter - 1) * mean
          + kappacPowerBefore * (kappacPowerAfter * kappacPowerAfter - 1) * priceBefore
          + kappacPowerAfter * (kappacPowerBefore * kappacPowerBefore - 1) * priceAfter)
          / (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter - 1);
      double stepVariance = (kappacPowerBefore * kappacPowerBefore - 1)
          * (kappacPowerAfter * kappacPowerAfter - 1) / ((kappac * kappac - 1)
              * (kappacPowerBefore * kappacPowerBefore * kappacPowerAfter * kappacPowerAfter - 1));
      return Gaussian.withMeanVariance(stepMean, stepVariance * shockVar).sample(rand);
    }

    // Calculated using mathematica.
    @Override
    public double expectedAverageIntermediateRmsd(double fundamentalZero, double fundamentalEnd,
        double price, long timeA, long timeB, long end) {
      double temp1 = Math.pow(kappac, 2);
      double temp2 = temp1 - 1;
      double temp3 = 2 * timeA;
      double temp4 = timeA - timeB - 1;
      double temp5 = 2 * end;
      double temp6 = Math.pow(kappac, temp5);
      double temp7 = temp6 - 1;
      double temp8 = kappac - 1;
      double temp9 = 1 + kappac;
      double temp10 = Math.pow(temp9, 2);
      double temp11 = Math.pow(kappac, end);
      double temp12 = mean - price;
      double temp13 = timeA + timeB;
      double temp14 = Math.pow(kappac, temp3);
      double temp15 = 2 * timeB;
      double temp16 = Math.pow(kappac, 2 + temp15);
      double temp17 = -mean;
      double temp18 = fundamentalEnd + temp17;
      double temp19 = -temp14;
      double temp20 = Math.pow(kappac, 4 * end);
      double temp21 = Math.pow(kappac, timeA);
      double temp22 = 2 * temp13;
      double temp23 = Math.pow(kappac, 1 + timeB);
      double temp24 = Math.pow(kappac, timeB);
      double temp25 = temp17 + price;
      double temp26 = -1 + temp11;
      double temp27 = Math.pow(kappac, 2 + temp3 + 4 * timeB);
      double temp28 = Math.pow(mean, 2);
      double temp29 = Math.pow(kappac, temp15);
      double temp30 = temp11 * temp18 + mean;
      double temp31 = fundamentalEnd + temp26 * mean;
      double temp32 = Math.pow(temp18, 2);
      double temp33 = -shockVar;
      double temp34 = Math.pow(fundamentalEnd, 2);
      double temp35 = temp34 * temp2 + 2 * fundamentalEnd * temp2 * temp26 * mean
          + temp2 * Math.pow(temp26, 2) * temp28 + shockVar - temp6 * shockVar;
      double temp36 = -2 * temp11 * temp2 * temp18 * mean;
      double temp37 = Math.pow(kappac, 3 * end);
      double temp38 = temp2 * Math.pow(temp12, 2);
      return (Math.pow(fundamentalZero, 2) * temp2
          * (Math.pow(kappac, 2 * (temp3 + timeB)) - temp27
              - 2 * temp4 * Math.pow(kappac, 2 * (timeA + timeB + end)) * temp2
              + temp20 * (temp14 - temp16))
          - 2 * temp8 * Math.pow(kappac, 1 + temp3 + 3 * timeB) * temp10 * temp7 * temp30 * temp12
          - 2 * temp8 * Math.pow(kappac, temp3 + timeB + end) * temp10 * temp7 * temp31 * temp12
          + 2 * fundamentalZero * temp2
              * (Math.pow(kappac, temp22 + end)
                  * (1 + timeB - temp1 - timeB * temp1 + temp19 + temp16 + timeA * temp2) * temp18
                  + temp37 * (temp19 + temp29 * (temp1 + temp4 * temp14 * temp2)) * temp18
                  + temp20 * (temp21 - temp23)
                      * (-(temp21 * mean) + temp24 * (-(kappac * mean) + temp21 * temp9 * temp12))
                  + Math.pow(kappac, temp22)
                      * (-temp21 + temp23)
                      * ((-1 - kappac + temp21 + temp23) * mean + price + kappac * price)
                  + Math.pow(kappac, timeA + timeB + temp5) * temp9
                      * (Math.pow(kappac, 1 + timeA + temp15) * temp12 + temp21 * temp25
                          + temp24 * (2 * temp4 * temp8 * temp21 * mean + kappac * temp12
                              + temp14 * temp25)))
          + Math.pow(kappac, 2 * (timeA + end)) * temp35
          + temp27
              * (temp36 + temp28 - temp1 * temp28 + temp33 + temp6 * (-(temp2 * temp32) + shockVar))
          + temp29 * (2 * temp8 * Math.pow(kappac, 3 * timeA) * temp10 * temp7 * temp30 * temp12
              + 2 * temp8 * Math.pow(kappac, 1 + timeA + end) * temp10 * temp7 * temp31 * temp12
              + Math.pow(kappac, 4 * timeA) * (2 * temp11 * temp2 * temp18 * mean + temp2 * temp28
                  + temp6 * (temp2 * temp32 + temp33) + shockVar)
              - Math.pow(kappac, 2 + temp5) * temp35
              + temp4 * temp14 * temp2
                  * (temp36 - 2 * temp37 * temp2 * temp18 * mean + temp38
                      - 2 * temp6 * temp2
                          * (temp34 - 2 * fundamentalEnd * mean + 3 * temp28 - 2 * mean * price
                              + Math.pow(price, 2))
                      + temp33 + temp20 * (temp38 + shockVar))))
          / (temp4 * Math.pow(kappac, 2 * temp13) * Math.pow(temp2, 2) * Math.pow(temp7, 2));
    }

    private static final long serialVersionUID = 1;

  }

  private static class FundamentalObservation extends Number implements Serializable {

    final double price;

    private FundamentalObservation(double price) {
      this.price = price;
    }

    @Override
    public double doubleValue() {
      return price;
    }

    @Override
    public float floatValue() {
      return (float) price;
    }

    @Override
    public int intValue() {
      return (int) price;
    }

    @Override
    public long longValue() {
      return (long) price;
    }

    @Override
    public String toString() {
      return Double.toString(price);
    }

    private static final long serialVersionUID = 1;


  }

  private static class JumpFundamentalObservation extends FundamentalObservation {

    long jumpsBefore;

    private JumpFundamentalObservation(double price, long jumps) {
      super(price);
      this.jumpsBefore = jumps;
    }

    @Override
    public String toString() {
      return "(" + price + ", " + jumpsBefore + ")";
    }

    private static final long serialVersionUID = 1;

  }

  private static final long serialVersionUID = 1;

}
