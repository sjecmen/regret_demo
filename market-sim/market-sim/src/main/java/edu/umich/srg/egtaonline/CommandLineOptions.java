package edu.umich.srg.egtaonline;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Once;

import javax.inject.Inject;

@Command(name = "egta", description = "Run this egta online simulator.")
public class CommandLineOptions {

  @Inject
  public HelpOption<?> help;

  @Option(name = {"-s", "--spec"}, title = "simulation-spec",
      description = "Path to simulation spec. (default: stdin)")
  public String simSpec = "-";

  @Option(name = {"-o", "--obs"},
      description = "Path to observaton file location. (default: stdout)")
  public String observations = "-";

  @Option(name = {"-j", "--jobs"}, title = "num-jobs",
      description = "Number of threads to use for processing. 0 implies number of cores."
          + " (default: 0)")
  public int jobs = 0;

  @Option(name = {"-p", "--sims-per-obs"}, title = "simulations-per-observation",
      description = "Number of simulations to use for one observation. Greater than one implies "
          + "\"no-features\". (default: 1)")
  public int simsPerObs = 1;

  @Option(name = "--no-features", description = "Don't compute features.")
  public boolean noFeatures = false;

  @Once
  @Arguments(title = "num-observations",
      description = "The number of observations to gather from the simulation spec."
          + " If multiple simulation specs are passed in, this many observations"
          + " will be sampled for each. (default: 1)")
  public int numObs = 1;

}
