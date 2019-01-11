package edu.umich.srg.egtaonline;

import com.google.gson.JsonObject;

import java.util.Collection;

public interface Observation {

  Collection<? extends Player> getPlayers();

  JsonObject getFeatures();

  interface Player {

    String getRole();

    String getStrategy();

    double getPayoff();

    JsonObject getFeatures();

  }

}
