#!/bin/bash
# usage: < sim_spec_comp.json ./market-sim.sh [options] <num-obs> > observations.json
#
# Script to run simulator efficiently from the command line. Input simulations
# specs are in the condensed strategy format

# Fail if bad stuff happens
set -euf -o pipefail

# Current directory
DIR="$(dirname "$0")"

# Execution
java -Xms1G -Xmx4G -jar "$DIR/target/marketsim-4.0.0-jar-with-dependencies.jar" $@
