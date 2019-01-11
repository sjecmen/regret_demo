package edu.umich.srg.learning;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;

import edu.umich.srg.distributions.Distribution;
import edu.umich.srg.distributions.Multinomial;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class ExponentialWeights<T> implements Distribution<T> {

  final Map<T, Double> weights;
  private final double learningRate;
  private final double logN;
  private int time;
  private Multinomial<T> distribution;

  private ExponentialWeights(double learningRate, Iterator<T> experts) {
    this.weights = new LinkedHashMap<>();
    for (T expert : (Iterable<T>) () -> experts) {
      weights.put(expert, 1d);
    }
    checkArgument(!weights.isEmpty());
    normalizeWeights(weights.size());

    this.learningRate = learningRate;
    this.logN = Math.log(weights.size());
    this.time = 1;
    this.distribution = null;
  }

  public static <T> ExponentialWeights<T> create(double learningRate, Iterator<T> experts) {
    return new ExponentialWeights<>(learningRate, experts);
  }

  public static <T> ExponentialWeights<T> create(double learningRate, Iterable<T> experts) {
    return new ExponentialWeights<>(learningRate, experts.iterator());
  }

  public static <T extends Number> NumericExponentialWeights<T> createNumeric(double learningRate,
      Iterator<T> experts) {
    return new NumericExponentialWeights<>(learningRate, experts);
  }

  public static <T extends Number> NumericExponentialWeights<T> createNumeric(double learningRate,
      Iterable<T> experts) {
    return new NumericExponentialWeights<>(learningRate, experts.iterator());
  }

  private void normalizeWeights(double total) {
    for (Entry<T, Double> e : weights.entrySet()) {
      e.setValue(e.getValue() / total);
    }
  }

  private void internalUpdate(Iterator<Entry<Entry<T, Double>, ? extends Number>> entrysAndGains) {
    double eta = learningRate * Math.min(1, Math.sqrt(logN / time));
    double total = 0;
    while (entrysAndGains.hasNext()) {
      Entry<Entry<T, Double>, ? extends Number> entry = entrysAndGains.next();
      double newWeight = entry.getKey().getValue() * Math.exp(eta * entry.getValue().doubleValue());
      entry.getKey().setValue(newWeight);
      total += newWeight;
    }
    normalizeWeights(total);
    assert DoubleMath.fuzzyEquals(1, weights.values().stream().mapToDouble(d -> d).sum(), 1e-3);
    time++;
    distribution = null;
  }

  /** Update the exponential weights algorithm with gains in insertion order. */
  public void update(Iterator<? extends Number> gains) {
    Iterator<Entry<T, Double>> experts = weights.entrySet().iterator();
    internalUpdate(new Iterator<Entry<Entry<T, Double>, ? extends Number>>() {

      @Override
      public boolean hasNext() {
        if (gains.hasNext() ^ experts.hasNext()) { // They disagree
          throw new IllegalArgumentException(
              "Update set must have the same number of experts as the original map");
        } else {
          return experts.hasNext();
        }
      }

      @Override
      public Entry<Entry<T, Double>, ? extends Number> next() {
        return new AbstractMap.SimpleImmutableEntry<>(experts.next(), gains.next());
      }

    });
  }

  /** Update the exponential weights algorithm with gains in insertion order. */
  public void update(Iterable<? extends Number> gains) {
    update(gains.iterator());
  }

  /** Update the exponential weights algorithm with gains with a mapping. */
  public void update(Map<T, ? extends Number> mappedGains) {
    Iterator<? extends Entry<T, ? extends Number>> gains = mappedGains.entrySet().iterator();
    internalUpdate(new Iterator<Entry<Entry<T, Double>, ? extends Number>>() {

      @Override
      public boolean hasNext() {
        return gains.hasNext();
      }

      @Override
      public Entry<Entry<T, Double>, ? extends Number> next() {
        Entry<T, ? extends Number> gain = gains.next();
        return new AbstractMap.SimpleImmutableEntry<>(
            new AbstractMap.SimpleImmutableEntry<>(gain.getKey(), weights.get(gain.getKey())),
            gain.getValue());
      }

    });
  }

  /** Return a probability / weight for each expert. */
  public Map<T, Double> getProbabilities() {
    return Collections.unmodifiableMap(weights);
  }

  /** Get counts for strategies that sum to total according to weights. */
  public Map<T, Integer> getCounts(int total, Random rand) {
    double[] newWeights = new double[weights.size()];
    int[] counts = new int[weights.size()];

    // Multiply weights but total counts and remove any integer parts
    int countLeft = total;
    int ind = 0;
    for (double weight : weights.values()) {
      weight *= total;
      int count = (int) Math.floor(weight);
      countLeft -= count;
      counts[ind] = count;
      newWeights[ind] = weight - count;
      ind++;
    }

    // Normalize by number of times we'll sample
    for (ind = 0; ind < newWeights.length; ++ind) {
      newWeights[ind] /= countLeft;
    }

    // Sample remaining ints
    IntDistribution extras = Multinomial.withWeights(newWeights);
    for (ind = 0; ind < countLeft; ++ind) {
      counts[extras.sample(rand)]++;
    }

    // Build map of results
    ImmutableMap.Builder<T, Integer> mappedCounts = ImmutableMap.builder();
    Iterator<T> elements = weights.keySet().iterator();
    for (ind = 0; ind < counts.length; ++ind) {
      mappedCounts.put(elements.next(), counts[ind]);
    }
    return mappedCounts.build();
  }

  /** Return an expert with probability proportional to it's weight. */
  @Override
  public T sample(Random rand) {
    if (distribution == null) {
      distribution = Multinomial.fromMap(weights);
    }
    return distribution.sample(rand);
  }

  public static class NumericExponentialWeights<T extends Number> extends ExponentialWeights<T> {
    private NumericExponentialWeights(double learningRate, Iterator<T> experts) {
      super(learningRate, experts);
    }

    /** Returns the expected expert value. */
    public double expectedValue() {
      return weights.entrySet().stream().mapToDouble(e -> e.getKey().doubleValue() * e.getValue())
          .sum();
    }
  }

}
