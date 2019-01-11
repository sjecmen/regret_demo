package edu.umich.srg.collect;

import java.util.DoubleSummaryStatistics;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class Collectors {

  public static DoubleArraySummaryStatistics arraySummaryStatistics(int length) {
    return new DoubleArraySummaryStatistics(length);
  }

  // TODO Generalize this to arbitrary consumers or collectors or something
  private static class DoubleArraySummaryStatistics
      implements Collector<double[], DoubleSummaryStatistics[], DoubleSummaryStatistics[]> {

    private final int length;

    private DoubleArraySummaryStatistics(int length) {
      this.length = length;
    }

    @Override
    public BiConsumer<DoubleSummaryStatistics[], double[]> accumulator() {
      return (summStats, element) -> {
        for (int i = 0; i < length; i++) {
          summStats[i].accept(element[i]);
        }
      };
    }

    @Override
    public Set<Characteristics> characteristics() {
      return EnumSet.of(Characteristics.IDENTITY_FINISH, Characteristics.UNORDERED);
    }

    @Override
    public BinaryOperator<DoubleSummaryStatistics[]> combiner() {
      return (summStats1, summStats2) -> {
        for (int i = 0; i < length; i++) {
          summStats1[i].combine(summStats2[i]);
        }
        return summStats1;
      };
    }

    @Override
    public Function<DoubleSummaryStatistics[], DoubleSummaryStatistics[]> finisher() {
      return Function.identity();
    }

    @Override
    public Supplier<DoubleSummaryStatistics[]> supplier() {
      return () -> Stream.generate(DoubleSummaryStatistics::new).limit(length)
          .toArray(DoubleSummaryStatistics[]::new);
    }

  }
}
