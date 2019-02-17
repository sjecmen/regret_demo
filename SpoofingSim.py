import numpy as np
import json
import subprocess

spoofing_strategy_names = ["markov:rmin_0_rmax_1000_thresh_0.4", "markov:rmin_0_rmax_1000_thresh_0.8", "markov:rmin_0_rmax_1000_thresh_1", "markov:rmin_0_rmax_2000_thresh_0.4", "markov:rmin_0_rmax_2000_thresh_0.8", "markov:rmin_0_rmax_2000_thresh_1", "markov:rmin_250_rmax_500_thresh_0.4", "markov:rmin_250_rmax_500_thresh_0.8", "markov:rmin_250_rmax_500_thresh_1", "hbl:rmin_250_rmax_500_thresh_1_NumTransactions_2", "hbl:rmin_250_rmax_500_thresh_1_NumTransactions_3", "hbl:rmin_250_rmax_500_thresh_1_NumTransactions_5", "hbl:rmin_250_rmax_500_thresh_1_NumTransactions_8"]
spoofing_num_strats = len(spoofing_strategy_names)
spoofing_num_players = 64

def normalize_payoffs(payoff, emp_min, emp_max): # normalize to 0, 1
    real_min = emp_min
    real_max = emp_max

    norm = (payoff - real_min) / (real_max - real_min)
    if norm > 1:
        norm = 1
    if norm < 0:
        norm = 0
    return norm

def load_distribution(market): # take 10000 * 65 samples, 95% within this range
    if market == "LSHN":
        return -3043.99065341657, 5990.580740809464
    elif market == "MSMN":
        return -8391.458334223938, 11254.361654069282
    elif market == "HSLN":
        return -12600.61541264787, 15370.989031487652 


def sample_spoofing_simulation(strat, mix, emp_min, emp_max, market):
    playersList = sample_spoofing_simulation_unnorm(strat, mix, market)

    payoffList = []
    for playerMap in playersList:
        if playerMap["strategy"] == spoofing_strategy_names[strat]:
            payoffList.append(playerMap["payoff"])
    payoff = np.random.choice(payoffList)

    return normalize_payoffs(payoff, emp_min, emp_max)


def sample_spoofing_simulation_unnorm(strat, mix, market): # returns list of player maps, which have strat and payoff
    # realize mix
    profile = np.random.multinomial(spoofing_num_players - 1, mix)
    profile[strat] += 1

    # write spec
    with open('sample_spec_' + market + '.json') as infile: # sample spec must already be created, containing configuration
        spec = json.load(infile)
        config = spec["configuration"]

    bg = {}
    for i, strat in enumerate(spoofing_strategy_names):
        bg[strat] = int(profile[i])
    spec["assignment"]["background"] = bg
    with open('spec.json', 'w') as specfile:
        json.dump(spec, specfile)

    # run simulation
    subprocess.run("< spec.json ./market-sim/market-sim/market-sim.sh 1 > output.json", shell=True)

    # read payoffs
    payoffs = np.zeros(len(mix))
    with open('output.json') as outfile:
        data = json.load(outfile)
    playersList = data["players"]

    return playersList
