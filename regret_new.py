import numpy as np
import sys
from Scenario import ToyGame
from Scenario import Scenario
from Algorithm import Algorithm

''' USAGE - command line args
algo_name: workshop, opt, or uniform
bound_name: lil or hoeffding
scenario_name: one of the scenarios specified in Scenario.py
'''
def main(algo_name, bound_name, scenario_name):
    scenario = Scenario(scenario_name)
    algo = Algorithm.make(algo_name, bound_name, scenario)
    print("Running scenario", scenario_name, "with algorithm", algo_name, "and bound", bound_name)

    w = float("inf")
    K = scenario.game.size()
    means = np.zeros((K))
    samples = np.zeros((K))
    for i in range(K):
         samples[i] = algo.startup 
         for t in range(algo.startup):
             means[i] += scenario.game.sample(i)
         means[i] /= samples[i] 
    while algo.width(means, samples) > scenario.W:
#        print(algo.width(means, samples))
        j = algo.sample(means, samples)
        means[j] = ((means[j] * samples[j]) + scenario.game.sample(j)) / (samples[j] + 1)
        samples[j] += 1
    print("samples taken:", sum(samples))
    print("final bounds:", means + algo.bound_superarms(means, samples))
    print("samples:", samples)
    print("widths:", algo.bound_superarms(means, samples))
    print("true means:", scenario.game.means)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
