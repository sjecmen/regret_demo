package edu.umich.srg.egtaonline;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.umich.srg.egtaonline.spec.Spec;

import java.util.Map.Entry;
import java.util.Objects;

public class SimSpec {

  public final Multiset<RoleStrat> assignment;
  public final Spec configuration;

  private SimSpec(Multiset<RoleStrat> assignment, Spec configuration) {
    this.assignment = assignment;
    this.configuration = configuration;
  }

  /** Read a SimSpec object from a reader. */
  public static SimSpec read(JsonObject obj, String classPrefix, CaseFormat keyCaseFormat) {
    ImmutableMultiset.Builder<RoleStrat> assignment = ImmutableMultiset.builder();
    for (Entry<String, JsonElement> role : obj.get("assignment").getAsJsonObject().entrySet()) {
      for (Entry<String, JsonElement> strategy : role.getValue().getAsJsonObject().entrySet()) {
        assignment.addCopies(new RoleStrat(role.getKey(), strategy.getKey()),
            strategy.getValue().getAsInt());
      }
    }

    Spec.Builder configuration = Spec.builder(classPrefix, keyCaseFormat);
    for (Entry<String, JsonElement> e : obj.get("configuration").getAsJsonObject().entrySet()) {
      configuration.put(e.getKey(), e.getValue().getAsString());
    }

    return new SimSpec(assignment.build(), configuration.build());
  }

  public static SimSpec create(Multiset<RoleStrat> assignment, Spec configuration) {
    return new SimSpec(assignment, configuration);
  }

  @Override
  public String toString() {
    return String.format("{assignment=%s, configuration=%s}", assignment, configuration);
  }

  public static class RoleStrat {

    private final String role;
    private final String strategy;

    private RoleStrat(String role, String strategy) {
      this.role = role;
      this.strategy = strategy;
    }

    public static RoleStrat of(String role, String strategy) {
      return new RoleStrat(role, strategy);
    }

    public String getRole() {
      return role;
    }

    public String getStrategy() {
      return strategy;
    }

    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof RoleStrat)) {
        return false;
      } else {
        RoleStrat that = (RoleStrat) other;
        return Objects.equals(this.role, that.role) && Objects.equals(this.strategy, that.strategy);
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(role, strategy);
    }

    @Override
    public String toString() {
      return role + ": " + strategy;
    }

  }

}
