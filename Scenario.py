import numpy as np
import SpoofingSim

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
        elif name == "boundedC": 
            W = 0.05
            means = [0.5, 0.42, 0.42, 0.42, 0.42, 0.42]
            for i in range(14):
                means.append(0.38)
            assert(len(means))
            std = 1/4
            mix = [0.05 for i in range(20)]
            self.game = BoundedGame(means, std)
        elif name == "spoofing":
            self.game = SpoofingGame()
            W = 0.05
            mix = np.random.random((SpoofingSim.spoofing_num_strats))
            mix /= sum(mix)
            print("spoofing mix:", mix)
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

    def sample(self, strat, mix): # returns a payoff of the deviation payoff for strategy strat
        return np.random.normal(self.means[strat], self.std)

    def i_star(self): # returns the best deviation
        return np.argmax(self.means)

    def size(self): # returns number of arms
        return len(self.means)

    def subg(self):
        return self.std

    def isBounded(self):
        return False


# Bernoulli variables, payoffs bounded to [0, 1].
# self.subg or std = 1/4, since subg of a bounded variable is (b-a)^2 / 4
class BoundedGame(ToyGame):
    def __init__(self, means, std):
        super(BoundedGame, self).__init__(means, 1/4)
        assert(all([mean <= 1 and mean >= 0 for mean in means]))

    def sample(self, strat, mix): # returns a payoff of the deviation payoff for strategy strat
        return np.random.binomial(1, self.means[strat]) 

    def isBounded(self):
        return True


class SpoofingGame():
    def __init__(self):
        self.emp_min, self.emp_max, _ = SpoofingSim.load_distribution()

    def sample(self, strat, mix):
        payoffs = SpoofingSim.sample_spoofing_simulation(strat, mix, self.emp_min, self.emp_max)
        return payoffs[strat]

    def subg(self):
        return 1/4

    def isBounded(self):
        return True

    def size(self): # returns number of arms
        return SpoofingSim.spoofing_num_strats

    def regret(self, mix):
        return None # true means unknown
