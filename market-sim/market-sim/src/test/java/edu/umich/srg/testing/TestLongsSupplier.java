package edu.umich.srg.testing;

import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestLongsSupplier extends ParameterSupplier {
  @Override
  public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
    TestLongs testLongs = sig.getAnnotation(TestLongs.class);
    long[] longs = testLongs.value();
    String name = Arrays.toString(longs);
    List<PotentialAssignment> list = new ArrayList<>(longs.length);
    for (long i : longs)
      list.add(PotentialAssignment.forValue(name, i));
    return list;
  }
}
