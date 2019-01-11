package edu.umich.srg.distributions;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Methods for creating multinomial distributions. Multinomial distributions over things other than
 * integers are created by first creating an IntMultinomial, and then converting that into a
 * distribution.
 */
public final class Multinomial<T> implements Distribution<T> {

  private final IntMultinomial multinomial;
  private final List<T> elements;

  private Multinomial(IntMultinomial multinomial, Iterable<T> items) {
    this.elements = ImmutableList.copyOf(items);
    checkArgument(elements.size() == multinomial.weights.length);
    this.multinomial = multinomial;
  }

  @Override
  public T sample(Random rand) {
    return elements.get(multinomial.sample(rand));
  }

  /** Create a multinomial from a map to doubles. */
  public static <T> Multinomial<T> fromMap(Map<T, ? extends Number> map) {
    ImmutableList.Builder<T> elements = ImmutableList.builder();
    ImmutableList.Builder<Double> weights = ImmutableList.builder();
    for (Entry<T, ? extends Number> e : map.entrySet()) {
      elements.add(e.getKey());
      weights.add(e.getValue().doubleValue());
    }
    return withWeights(weights.build()).over(elements.build());
  }

  public static IntMultinomial withWeights(Collection<? extends Number> weights) {
    return new IntMultinomial(weights);
  }

  public static IntMultinomial withWeights(double... weights) {
    return new IntMultinomial(Doubles.asList(weights));
  }

  public static class IntMultinomial implements IntDistribution {
    private final double[] weights;

    private IntMultinomial(Collection<? extends Number> weights) {
      this.weights = Doubles.toArray(weights);

      for (int i = 1; i < this.weights.length; ++i) {
        this.weights[i] += this.weights[i - 1];
      }
      for (int i = 0; i < this.weights.length; ++i) {
        this.weights[i] /= this.weights[this.weights.length - 1];
      }
    }

    @Override
    public int sample(Random rand) {
      int index = Arrays.binarySearch(weights, rand.nextDouble());
      return index < 0 ? -index - 1 : index;
    }

    public <T> Multinomial<T> over(Collection<T> items) {
      return new Multinomial<>(this, items);
    }

  }

}
