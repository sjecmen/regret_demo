import numpy as np
import sys
from ToyGame import ToyGame
from ToyGame import Scenario
from Algorithm import Algorithm

def main(algo_name, game_name):
    scenario = Scenario(game_name)
    algo = Algorithm.make(algo_name, scenario)

    w = float("inf")
    K = scenario.game.size()
    means = np.zeros((K))
    samples = np.zeros((K))
    startup = 2
    for i in range(K):
         samples[i] = startup 
         for t in range(startup):
             means[i] += scenario.game.sample(i)
         means[i] /= samples[i] 
    while algo.width(means, samples) > scenario.W:
        print(algo.width(means, samples))
        j = algo.sample(means, samples)
        means[j] = ((means[j] * samples[j]) + scenario.game.sample(j)) / (samples[j] + 1)
        samples[j] += 1
    print("samples taken:", sum(samples))

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
