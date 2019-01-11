package edu.umich.srg.util;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import edu.umich.srg.testing.Repeat;
import edu.umich.srg.testing.RepeatRule;

import java.util.PrimitiveIterator.OfDouble;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class LinearTest {

  @Rule
  public RepeatRule repeatRule = new RepeatRule();

  private static final Random rand = new Random();

  @Test
  public void linearRSquaredTest() {
    OfDouble x = DoubleStream.of(1, 2, 3, 4, 5).iterator();
    OfDouble y = DoubleStream.of(5, 7, 9, 11, 13).iterator();
    assertEquals(1, Linear.pearsonCoefficient(x, y), 1e-8);
  }

  @Test
  @Repeat(100)
  public void noCoorelationTest() {
    int n = 100000;
    OfDouble x = IntStream.range(0, n).asDoubleStream().iterator();
    OfDouble y = DoubleStream.generate(rand::nextDouble).limit(n).iterator();
    assertEquals(0, Linear.pearsonCoefficient(x, y), 1e-3);
  }

  @Test
  public void linearFitTest() {
    OfDouble x = DoubleStream.of(0, 0, 2, 2).iterator();
    OfDouble y = DoubleStream.of(0, 2, 2, 4).iterator();
    DoubleUnaryOperator fit = Linear.linearFit(x, y);
    assertEquals(1, fit.applyAsDouble(0), 1e-3);
    assertEquals(4, fit.applyAsDouble(3), 1e-3);
  }

}
