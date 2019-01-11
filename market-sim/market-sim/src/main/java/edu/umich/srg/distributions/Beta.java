package edu.umich.srg.distributions;

import edu.umich.srg.distributions.Distribution.DoubleDistribution;

import java.util.Random;

/** Allows sampling from a standard beta distribution. */
public abstract class Beta implements DoubleDistribution {

  /** Create a beta distribution. */
  public static Beta with(double alpha, double beta) {
    if (alpha == 1 && beta == 1) {
      return new Uniform();
    } else if (alpha == beta && alpha > 1.5) {
      return new AhrensDieterBs(alpha);
    } else {
      throw new IllegalArgumentException(
          "Alpha = " + alpha + " and Beta = " + beta + " not implemented yet");
    }
  }

  private static class Uniform extends Beta {

    @Override
    public double sample(Random rand) {
      return rand.nextDouble();
    }

  }

  /**
   * Rejection sampling approximation of symmetric beta. This method is an approximation that is
   * better at larger values of alpha.
   */
  private static class AhrensDieterBs extends Beta {

    private final double alpha;
    private final double alphac;
    private final double sqrtAlpha;

    private AhrensDieterBs(double alpha) {
      this.alpha = alpha;
      this.alphac = alpha - 1;
      this.sqrtAlpha = Math.sqrt(alpha);
    }

    @Override
    public double sample(Random rand) {
      double correctedGaussian;
      double normal;
      double normal4;
      double uniform;

      do {
        do {
          normal = rand.nextGaussian();
          correctedGaussian = 0.5 * (1 + normal / sqrtAlpha);
        } while (correctedGaussian < 0 || correctedGaussian > 1);
        uniform = rand.nextDouble();
        normal4 = normal * normal * normal * normal;
      } while (uniform > 1 - normal4 / (8 * alpha - 12) && (uniform >= 1 - normal4 / (8 * alpha - 8)
          + 0.5 * (normal4 / (8 * alpha - 8)) * (normal4 / (8 * alpha - 8))
          || Math.log1p(uniform - 1) > alphac
              * Math.log1p(4 * correctedGaussian * (1 - correctedGaussian) - 1)
              + normal * normal / 2));
      return correctedGaussian;
    }
  }

  private Beta() {} // Unconstructable

}
