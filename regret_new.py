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
def main(algo_name, bound_name, scenario_name, setting_name):
    assert(setting_name == "bandit" or setting_name == "finite")
    scenario = Scenario(scenario_name)
    algo = Algorithm.make(algo_name, bound_name, scenario)
    print("Running scenario", scenario_name, "with algorithm", algo_name, "and bound", bound_name)

    num_iterations = 10000
    width_avg, sample_avg = run(scenario, algo, setting_name)
    for i in range(num_iterations - 1):
        width_history, sample_history = run(scenario, algo, setting_name)
        width_avg += width_history
        sample_avg += sample_history
    width_avg /= num_iterations
    sample_avg /= num_iterations
    save_data(setting_name, algo_name, scenario_name, ["widths", "samples"], [width_avg, sample_avg])
        

def run(scenario, algo, setting_name):
    K = scenario.game.size()

    means = np.zeros((K))
    samples = np.zeros((K))
    for i in range(K):
         samples[i] = algo.startup 
         for t in range(algo.startup):
             means[i] += scenario.game.sample(i, algo.mix)
         means[i] /= samples[i] 

    T = 1000
    sample_history = np.zeros((T, K))
    width_history = np.zeros((T))

    t = 0
    while True:
        w = algo.width(means, samples)

        sample_history[t] = samples
        width_history[t] = w

        if (setting_name == "finite" and w <= scenario.W) or (setting_name == "bandit" and t == T):
            return width_history, sample_history

        if t % 100 == 0:
            print("t:", t, "w:", w)
        t += 1

        j = algo.sample(means, samples)
        means[j] = ((means[j] * samples[j]) + scenario.game.sample(j, algo.mix)) / (samples[j] + 1)
        samples[j] += 1


def save_data(setting, alg, name, labels, files):
    for i, label in enumerate(labels):
        np.save(label + "_" + alg + "_" + name + "_" + setting, files[i])


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])

