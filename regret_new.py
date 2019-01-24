import numpy as np
import sys
from Scenario import ToyGame
from Scenario import Scenario
from Algorithm import Algorithm

''' USAGE - command line args
algo_name: workshop, opt, uniform, etc
bound_name: lil or hoeffding, etc
scenario_name: one of the scenarios specified in Scenario.py
setting_name: bandit (fixed time) or finite (fixed width)
'''
def main(algo_name, bound_name, scenario_name, setting_name):
    assert(setting_name == "bandit" or setting_name == "finite")
    scenario = Scenario(scenario_name)
    algo = Algorithm.make(algo_name, bound_name, scenario)
    print("Running scenario", scenario_name, "with algorithm", algo_name, "and bound", bound_name)

    num_iterations = 100
    width_avg, sample_avg, correct_avg, UB_avg, mean_avg = run(scenario, algo, setting_name)
    for i in range(num_iterations - 1):
        if i % 10 == 0:
            print("iter:", i)
            save_data(setting_name, algo_name, scenario_name, ["widths"+i, "samples"+i, "correct"+i, "UB"+i, "mean"+i], [width_avg/(i+1), sample_avg/(i+1), correct_avg/(i+1), UB_avg/(i+1), mean_avg/(i+1)])
            print("w:", width_avg[-1]/(i+1))
        width_history, sample_history, correct_history, UB_history, mean_history = run(scenario, algo, setting_name)
        width_avg += width_history
        sample_avg += sample_history
        correct_avg += correct_history
        UB_avg += UB_history
        mean_avg += mean_history
    width_avg /= num_iterations
    sample_avg /= num_iterations
    correct_avg /= num_iterations
    UB_avg /= num_iterations
    mean_avg /= num_iterations
    save_data(setting_name, algo_name, scenario_name, ["widths", "samples", "correct", "UB", "mean"], [width_avg, sample_avg, correct_avg, UB_avg, mean_avg])
        

def run(scenario, algo, setting_name):
    K = scenario.game.size()

    means = np.zeros((K))
    samples = np.zeros((K))
    for i in range(K):
         samples[i] = algo.startup 
         for t in range(algo.startup):
             means[i] += scenario.game.sample(i, algo.mix)
         means[i] /= samples[i] 

    T = 250000
    sample_history = np.zeros((T, K))
    width_history = np.zeros((T))
    correct_history = np.zeros((T))
    UB_history = np.zeros((T))
    mean_history = np.zeros((T))

    t = 0
    while True:
        j = algo.sample(means, samples)
        means[j] = ((means[j] * samples[j]) + scenario.game.sample(j, algo.mix)) / (samples[j] + 1)
        samples[j] += 1

        w = algo.width(means, samples)

        if (setting_name == "bandit"):
            sample_history[t] = samples
            width_history[t] = w

            sa_means = means - np.dot(algo.mix, means)        
            bounds = algo.bound_superarms(means, samples) 
            upper_bounds = sa_means + bounds
            mean_history[t] = np.max(sa_means)
            UB_history[t] = np.max(upper_bounds)

            regret = scenario.game.regret(scenario.mix)
            if regret != None:
                emp_regret = np.max(means) - np.dot(algo.mix, means)
                inside = (regret <= emp_regret + scenario.W) and (regret >= emp_regret - scenario.W)
                correct_history[t] = 1 if inside else 0

        t += 1
        if (setting_name == "finite" and w <= scenario.W):
            print(t)
            return width_history, sample_history, correct_history, UB_history, mean_history
        elif (setting_name == "bandit" and t == T):
            return width_history, sample_history, correct_history, UB_history, mean_history


def save_data(setting, alg, name, bound, labels, files):
    for i, label in enumerate(labels):
        np.save(label + "_" + alg + "_" + name + "_" + bound + "_" + setting, files[i])


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])

