HFT Market Simulator
====================

The HFT Market Simulator is a discrete event java simulation for financial markets with high frequency traders.
It is also EGTA Online compatible.
All relevant code is located in the `market-sim` directory.
Legacy code is still preserved in some of the other directories.

Quick Start
-----------

We recommend developing on Unix-based (Mac or Linux) systems.
This guide was written based on that assumption.

First install the necessary dependencies (on mac use `brew`):
```
sudo apt-get install openjdk-8-jdk make maven jq git
```

You should have an account on [strategicreasoning.eecs.umich.edu](https://strategicreasoning.eecs.umich.edu).
Go [here](https://strategicreasoning.eecs.umich.edu/profile/keys) to add a key to your account.

Now checkout the git repository:
```
git clone git@strategicreasoning.eecs.umich.edu:ebrink/market-sim.git
```

Enter the simulator directory and verify all of the tests pass:
```
cd market-sim/market-sim
make test
```

From the same directory, compile the project:
```
make jar
```

Run a simple simulation and look at the output:
```
< resources/simulation_spec_condensed.json ./market-sim.sh 1 | jq .
```

Compile a zip file (called `defaults.zip`) for use with egtaonline:
```
make egta def=resources/defaults.json
```


Documentation Contents
----------------------

- [Developing](docs/developing.md) - A guide on how to begin modifying the simulator yourself.
- [Running](docs/running.md) - A guide on how to run the simulator a number of ways.
- [Jq](docs/jq.md) - A guide about using `jq`---a very useful command line tool for parsing and editing json.
- [Fundamental](docs/fundamental.md) - General mathematical information about the Gaussian mean-reverting fundamental process we use in the simulator.
- [Keys](docs/keys.md) - Auto-generated documentation on all of the keys available to the simulator.
