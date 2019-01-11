package edu.umich.srg.testing;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestIntsSupplier extends ParameterSupplier {
  @Override
  public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
    TestInts testInts = sig.getAnnotation(TestInts.class);
    int[] ints = testInts.value();
    String name = Arrays.toString(ints);
    List<PotentialAssignment> list = new ArrayList<>(ints.length);
    for (int i : ints)
      list.add(PotentialAssignment.forValue(name, i));
    return list;
  }
}
