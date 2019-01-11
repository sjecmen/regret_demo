package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import edu.umich.srg.distributions.Distribution.IntDistribution;

import java.io.Serializable;
import java.util.Random;

public abstract class Hypergeometric implements IntDistribution, Serializable {

  /** Create a hypergeometric distribution for sampling. */
  public static Hypergeometric with(int populationSize, int numSuccesses, int draws) {
    checkArgument(0 <= populationSize);
    checkArgument(0 <= numSuccesses && numSuccesses <= populationSize);
    checkArgument(0 <= draws && draws <= populationSize);

    int offset = 0;
    int sign = 1;
    if (2 * draws > populationSize) {
      draws = populationSize - draws;
      offset += sign * numSuccesses;
      sign *= -1;
    }
    if (2 * numSuccesses > populationSize) {
      numSuccesses = populationSize - numSuccesses;
      offset += sign * draws;
      sign *= -1;
    }
    return new OffsetHypergeometric(hypergeometricSwitch(populationSize, numSuccesses, draws),
        offset, sign);
  }

  private static Hypergeometric hypergeometricSwitch(int populationSize, int numSuccesses,
      int draws) {
    if (numSuccesses == 0 || draws == 0) {
      return new ConstantHypergeometric(0);
    } else if (draws == populationSize) {
      return new ConstantHypergeometric(numSuccesses);
    } else if (numSuccesses == populationSize) {
      return new ConstantHypergeometric(draws);
    } else if (populationSize > 15) {
      return new InverseCmfHypergeometric(populationSize, numSuccesses, draws);
    } else {
      return new BruteHypergeometric(populationSize, numSuccesses, draws);
    }
  }

  private static class InverseCmfHypergeometric extends Hypergeometric {

    private final InverseCmf invCmf;

    /**
     * Sample from the hypergeometric using an inverse cmf.
     * 
     * @param populationSize N population size
     * @param populationSuccesses K number of successes in population
     * @param sampleSize n number of draws
     */
    private InverseCmfHypergeometric(int populationSize, int populationSuccesses, int sampleSize) {
      double populationNotSampled = populationSize - sampleSize;
      double populationFailures = populationSize - populationSuccesses;
      double nkn = populationSize - populationSuccesses - sampleSize;
      double proability0 = Math.exp((populationFailures + 0.5) * Math.log(populationFailures)
          + (populationNotSampled + 0.5) * Math.log(populationNotSampled)
          - (nkn + 0.5) * Math.log(nkn) - (populationSize + 0.5) * Math.log(populationSize));
      this.invCmf = new InverseCmf(proability0,
          (InverseCmf.PmfFunction & Serializable) (probabilityPreviousSuccesses,
              drawnSuccesses) -> probabilityPreviousSuccesses
                  * (populationSuccesses - drawnSuccesses + 1) * (sampleSize - drawnSuccesses + 1)
                  / (drawnSuccesses * (nkn + drawnSuccesses)));
    }

    @Override
    public int sample(Random rand) {
      return invCmf.sample(rand);
    }

    private static final long serialVersionUID = 1;

  }

  /** Brute force sample the hypergeometric. */
  private static class BruteHypergeometric extends Hypergeometric {

    private final int populationSize;
    private final int numSuccesses;
    private final int draws;

    private BruteHypergeometric(int populationSize, int numSuccesses, int draws) {
      this.populationSize = populationSize;
      this.numSuccesses = numSuccesses;
      this.draws = draws;
    }

    @Override
    public int sample(Random rand) {
      int result = 0;
      int populationLeft = populationSize;
      int successesLeft = numSuccesses;

      for (int i = 0; i < draws; ++i) {
        if (rand.nextInt(populationLeft) < successesLeft) {
          ++result;
          --successesLeft;
        }
        --populationLeft;
      }
      return result;
    }

    private static final long serialVersionUID = 1;

  }

  /** A constant hypergeometric. */
  private static class ConstantHypergeometric extends Hypergeometric {

    private final int constant;

    private ConstantHypergeometric(int constant) {
      this.constant = constant;
    }

    @Override
    public int sample(Random rand) {
      return constant;
    }

    private static final long serialVersionUID = 1;

  }

  /** A hypergeometric that can be framed as an easier hypergeometric to sample from. */
  private static class OffsetHypergeometric extends Hypergeometric {
    private final Hypergeometric other;
    private final int offset;
    private final int sign;

    private OffsetHypergeometric(Hypergeometric other, int offset, int sign) {
      this.other = other;
      this.offset = offset;
      this.sign = sign;
    }

    @Override
    public int sample(Random rand) {
      return offset + sign * other.sample(rand);
    }

    private static final long serialVersionUID = 1;
  }

  private static final long serialVersionUID = 1;

}
