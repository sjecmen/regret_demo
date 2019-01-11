package edu.umich.srg.testing;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestStringsSupplier extends ParameterSupplier {
  @Override
  public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
    TestStrings testStrings = sig.getAnnotation(TestStrings.class);
    String[] strings = testStrings.value();
    String name = Arrays.toString(strings);
    List<PotentialAssignment> list = new ArrayList<>(strings.length);
    for (String i : strings)
      list.add(PotentialAssignment.forValue(name, i));
    return list;
  }
}
