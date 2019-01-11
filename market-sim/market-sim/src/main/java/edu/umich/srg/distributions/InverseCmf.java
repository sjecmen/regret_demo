package edu.umich.srg.distributions;

import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class InverseCmf implements Serializable {

  private double lastPmf;
  private final List<Double> cmf;
  private final PmfFunction nextPmf;

  InverseCmf(double zeroPmf, PmfFunction nextPmf) {
    this.lastPmf = zeroPmf;
    this.cmf = new ArrayList<>(Collections.singleton(zeroPmf));
    this.nextPmf = nextPmf;
  }

  public int sample(Random rand) {
    double invCmf = rand.nextDouble();
    double largestCmf = Iterables.getLast(cmf);
    if (invCmf < largestCmf) {
      int index = Collections.binarySearch(cmf, invCmf);
      return Math.abs(index + 1);
    } else {
      do {
        lastPmf = nextPmf.nextPmf(lastPmf, cmf.size());
        largestCmf += lastPmf;
        cmf.add(largestCmf);
      } while (invCmf >= largestCmf);
      return cmf.size() - 1;
    }
  }

  interface PmfFunction {

    double nextPmf(double previousPmf, int index);

  }

  private static final long serialVersionUID = 1;

}
