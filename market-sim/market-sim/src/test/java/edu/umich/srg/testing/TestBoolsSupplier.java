package edu.umich.srg.testing;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestBoolsSupplier extends ParameterSupplier {
  @Override
  public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
    TestBools testBools = sig.getAnnotation(TestBools.class);
    boolean[] bools = testBools.value();
    String name = Arrays.toString(bools);
    List<PotentialAssignment> list = new ArrayList<>(bools.length);
    for (boolean b : bools)
      list.add(PotentialAssignment.forValue(name, b));
    return list;
  }
}
