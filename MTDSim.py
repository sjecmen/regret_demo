import numpy as np
import json
import subprocess

mtd_strategy_names = [
    "periodicMax-1",
    "periodicMax-7",
    "probeCountTime-1_1",
    "probeCountTime-2_1", 
    "probeCountTime-2_50", 
    "probeCountTime-4_1", 
    "probeCountTime-4_50", 
    "No"] # control strategies? max-renewal?
mtd_num_strats = len(spoofing_strategy_names)
mtd_num_players = 1

def normalize_payoffs(payoff, emp_min, emp_max): # normalize to 0, 1
    real_min = emp_min
    real_max = emp_max

    norm = (payoff - real_min) / (real_max - real_min)
    if norm > 1:
        norm = 1
    if norm < 0:
        norm = 0
    return norm

def load_distribution(percent): # take 10000 * 65 samples, 95% within this range
        assert(False)


def sample_mtd_simulation(strat, mix, emp_min, emp_max):
    playersList = sample_spoofing_simulation_unnorm(strat, mix)
    for playerMap in playersList:
        if playerMap["role"] == "DEF" and playerMap["strategy"] == spoofing_strategy_names[strat]:
            payoff = playerMap["payoff"])
    return normalize_payoffs(payoff, emp_min, emp_max)


def sample_mtd_simulation_unnorm(strat, mix): # returns list of player maps, which have strat and payoff
    # realize mix, which means only a pure strat you deviate to
    profile = np.zeros(mtd_num_strats)
    profile[strat] += 1

    # write spec
    with open('sample_spec_mtd.json') as infile: # sample spec must already be created, containing configuration
        spec = json.load(infile)
    spec["assignment"]["DEF"] = [spoofing_strategy_names[strat]]
    with open('spec.json', 'w') as specfile:
        json.dump(spec, specfile)

    # run simulation
    subprocess.run("< spec.json RUN_CMD > output.json", shell=True) # TODO change

    # read payoffs
    with open('output.json') as outfile:
        data = json.load(outfile)
    playersList = data["players"]

    return playersList
