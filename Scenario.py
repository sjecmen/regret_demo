import numpy as np


# Holds game, mixed profile, and desired width parameters
class Scenario:
    def __init__(self, name):
        W = 0.5
        if name == "uniMeans":
            means = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
            std = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
            self.game = ToyGame(means, std)
        elif name == "closeTop":
            means = [10, 9.5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            std = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
            self.game = ToyGame(means, std)
        elif name == "bestInSupp":
            means = [10, 9.5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            std = 2
            mix = [0.75, 0, 0, 0, 0.25, 0, 0, 0, 0, 0]
            self.game = ToyGame(means, std)
        elif name == "fullSupp":
            means = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
            std = 2
            mix = [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1]
            self.game = ToyGame(means, std)
        elif name == "closerTop":
            means = [10, 9.9, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1]
            std = 2
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
            self.game = ToyGame(means, std)
        elif name == "bounded": 
            W = 0.05
            means = [0.75, 0.7, 0.65, 0.6, 0.55, 0.45, 0.4, 0.35, 0.3, 0.25]
            std = 1/4
            mix = [0, 0, 0, 0, 0.25, 0, 0, 0, 0.5, 0.25]
            self.game = BoundedGame(means, std)
        else:
            assert(False)
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

    def isBounded(self):
        return False

# As above, but payoffs bounded to [0, 1].
# When using BoundedGame, algorithms will only know of the boundedness in [0,1], not the true std. Therefore, self.subg = 1/4, since subg of a bounded variable is (b-a)^2 / 4
class BoundedGame(ToyGame):
    def __init__(self, means, std):
        super(BoundedGame, self).__init__(means, std)
        self.min = 0
        self.max = 1
        assert(all([mean <= self.max and mean >= self.min for mean in means]))

    def sample(self, strat): # returns a payoff of the deviation payoff for strategy strat
        return np.clip(np.random.normal(self.means[strat], self.std), self.min, self.max)

    def subg(self):
        return (self.max - self.min)**2 / 4

    def isBounded(self):
        return True

