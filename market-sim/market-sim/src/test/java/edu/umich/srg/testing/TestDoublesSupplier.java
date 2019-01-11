package edu.umich.srg.testing;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestDoublesSupplier extends ParameterSupplier {
  @Override
  public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
    TestDoubles testDoubles = sig.getAnnotation(TestDoubles.class);
    double[] doubles = testDoubles.value();
    String name = Arrays.toString(doubles);
    List<PotentialAssignment> list = new ArrayList<>(doubles.length);
    for (double i : doubles)
      list.add(PotentialAssignment.forValue(name, i));
    return list;
  }
}
