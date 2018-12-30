import numpy as np


# Holds game, mixed profile, and desired width parameters
class Scenario:
    def __init__(self, name):
        W = 0.5
        if name == "uniMeans":
            means = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
            subg = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
        elif name == "closeTop":
            means = [10, 9.5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            subg = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
        elif name == "bestInSupp":
            means = [10, 9.5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            subg = 2
            mix = [0.75, 0, 0, 0, 0.25, 0, 0, 0, 0, 0]
        elif name == "fullSupp":
            means = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
            subg = 2
            mix = [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1]
        elif name == "closerTop":
            means = [10, 9.9, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            subg = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
        else:
            assert(False)
        self.game = ToyGame(means, subg)
        self.mix = mix
        self.W = W


# Game with gaussian deviation payoffs for all strategies. Gaussians have specified means and identical standard deviation.
class ToyGame:
    def __init__(self, means, std):
        self.means = means
        self.std = std
        self.num_strats = np.size(means)

    def regret(self, mix): # returns the true regret with respect to symmetric mixed strategy mix
        return np.max(self.means) - np.dot(self.means, mix)

    def sample(self, strat): # returns a payoff of the deviation payoff for strategy strat
        return np.random.normal(self.means[strat], self.std)

    def i_star(self): # returns the best deviation
        return np.argmax(self.means)

    def size(self): # returns number of arms
        return len(self.means)

    def subg(self):
        return self.std



