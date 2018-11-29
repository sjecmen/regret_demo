import numpy as np
import sys

class ToyGame: # gaussian payoffs
    def __init__(self, means, std):
        self.means = means
        self.std = std
        self.num_strats = np.size(means)

    def regret(self, mix):
        return np.max(self.means) - np.dot(self.means, mix)

    def sample(self, strat):
        return np.random.normal(self.means[strat], self.std)

    def i_star(self):
        return np.argmax(self.means)


def main(setting, name):
#    run(setting, "greedy", name)
#    run(setting, "uniform", name)
#    run(setting, "optimal", name)
   run(setting, "UCB", name)

# Runs given algorithm with given setting and game name many iterations and averages results, then saves results to a file for output later.
# Parameters:
#  - alg: string indicating how to select arms
#  - setting: "bandit" or "finite", indicating whether we have fixed-bound-width or fixed-sample stopping criteria
#  - name: string indicating which game to create
def run(setting, alg, name):
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
    assert(len(means) == len(mix))

    game = ToyGame(means, subg)
    iterations = 100
    alpha = 0.05

    if setting == "bandit":
        T = 1000
        samples, widths, isc = bandit(game, mix, alpha, subg, alg, setting, T=T)
        for i in range(iterations - 1):
            results = bandit(game, mix, alpha, subg, alg, setting, T=T)
            samples += results[0]
            widths += results[1]
            isc += results[2]
        samples /= iterations
        widths /= iterations
        isc /= iterations
        save_data(setting, alg, name, (samples, widths, isc))
    elif setting == "finite":
        z = 0.5
        K = 10
        t, UB, LB = bandit(game, mix, alpha, subg, alg, setting, z=z, K=K)
        for i in range(iterations - 1):
            results = bandit(game, mix, alpha, subg, alg, setting, z=z, K=K)
            t += results[0]
            UB += results[1]
            LB += results[2]
        t /= iterations
        UB /= iterations
        LB /= iterations
        save_data(setting, alg, name, (t, UB, LB))

# Runs algorithm and returns relevant information.
#     if setting == "bandit": returns [sample distribution at each time, bound width at each time, best deviation at each time]
#     if setting == "finite": returns [number of samples taken, final upper bound, final lower bound] 
# Parameters:
#  - game: object of ToyGame class
#  - mix: list of weights in mixed strategy
#  - alpha: total desired probability of error
#  - subg: subgaussian parameter for all payoffs
#  - alg: string indicating how to select arms
#  - setting: "bandit" or "finite", indicating whether we have fixed-bound-width or fixed-sample stopping criteria
#  - z: desired bound width
#  - K: number of times to check stopping criterion (for Bonferroni correction)
#  - T: maximum number of samples to take
def bandit(game, mix, alpha, subg, alg, setting, z=None, K=None, T=None):
    if setting == "bandit": # stopping criterion: T samples taken
        assert(z == None and K == None and T != None)
        K = 1
    elif setting == "finite": # stopping criterion: bound width less than z
        assert(z != None and K != None and T == None)
        T = static_bound_uniform(game, z, K, alpha, subg)
    else:
        assert(false)

    step = T // K
    stops = np.arange(step + (T % step) - 1, T, step)
    assert(len(stops) == K and T-1 in stops)

    delta = alpha / (2 * game.num_strats * K)

    sample_history = np.zeros((T, game.num_strats))
    bound_width_history = np.zeros((T))
    i_star_history = np.zeros((T))

    payoffs = np.zeros(game.num_strats)
    for s in range(game.num_strats):
        payoffs[s] = game.sample(s)
    samples = np.ones(game.num_strats)

    # Calculate bounds
    eq = np.dot(mix, payoffs)
    gains = payoffs - eq
    empirical_regret = gains.max()
    coef = np.abs(np.eye(game.num_strats) - np.tile(mix, (game.num_strats, 1)))
    elts = (coef**2) / np.tile(samples, (game.num_strats, 1))
    gains_bound = np.sqrt(-2 * subg**2 * np.log(delta) * np.sum(elts, 1))
    lower_gains = np.maximum(gains - gains_bound, 0)
    upper_gains = gains + gains_bound

    for t in range(T):
        # Sample one payoff
        i_star = np.argmax(upper_gains)
        if alg == "greedy": # workshop algorithm
            j_star = np.argmax(coef[i_star]**2 / samples**2)
        elif alg == "uniform":
            j_star = np.random.choice(game.num_strats)
        elif alg == "optimal": # with knowledge of best deviation
            j_star = np.random.choice(game.num_strats, p=coef[game.i_star()]/np.sum(coef[game.i_star()])) # TODO /0 if mix[i*]=1
        elif alg == "OAS": # samples randomly in the optimal distribution among the highest super-arm
            j_star = np.random.choice(game.num_strats, p=coef[i_star]/np.sum(coef[i_star]))
        elif alg == "UAS": # samples randomly in a uniform distribution among the highest super-arm
            iSupp = np.ceil(coef[i_star])
            j_star = np.random.choice(game.num_strats, p=iSupp/np.sum(iSupp))
        elif alg == "UCB": # from workshop, does pure identification for first half then does bound-minimization
            tt = np.searchsorted(stops, t)
            if tt == 0:
                prev = -1 
            else:
                prev = stops[tt - 1]
            Tt = stops[tt] - prev
            if t < prev + (Tt/2):
                payoff_bounds = np.sqrt(-2 * subg**2 * np.log(delta) / samples)
                j_star = np.argmax(payoffs + payoff_bounds)
            else:
                if t == prev + (np.ceil(Tt/2)):
                    payoff_bounds = np.sqrt(-2 * subg**2 * np.log(delta) / samples)
                    selected_i_star = np.argmax(payoffs + payoff_bounds)
                j_star = np.random.choice(game.num_strats, p=coef[selected_i_star]/np.sum(coef[selected_i_star]))
        else:
            assert(False)
        samples[j_star] += 1
        payoffs[j_star] += (game.sample(j_star) - payoffs[j_star]) / samples[j_star]

        # Calculate bounds
        eq = np.dot(mix, payoffs)
        gains = payoffs - eq
        empirical_regret = gains.max()
        coef = np.abs(np.eye(game.num_strats) - np.tile(mix, (game.num_strats, 1)))
        elts = (coef**2) / np.tile(samples, (game.num_strats, 1))
        gains_bound = np.sqrt(-2 * subg**2 * np.log(delta) * np.sum(elts, 1))
        lower_gains = np.maximum(gains - gains_bound, 0)
        upper_gains = gains + gains_bound

        # Store information
        sample_history[t] = samples
        bound_width_history[t] = np.max(upper_gains) - np.max(lower_gains)
        i_star_history[t] = i_star
        if alg == "optimal":
            bound_width_history[t] = upper_gains[game.i_star()] - lower_gains[game.i_star()]
            i_star_history[t] = game.i_star()

        # Check for stop
        if setting == "finite":
            if t in stops: 
                if alg != "optimal" and np.max(upper_gains) - np.max(lower_gains) <= z:
                    return t, np.max(upper_gains), np.max(lower_gains) 
                if alg == "optimal" and upper_gains[game.i_star()] - lower_gains[game.i_star()] <= z:
                    return t, upper_gains[game.i_star()], lower_gains[game.i_star()]

    if setting == "bandit":
        isc = np.array([np.sum(i_star_history[:t] == game.i_star())/t for t in range(1, T+1)])
        return sample_history, bound_width_history, isc
    elif setting == "finite":
        return None

# Determines the maximum number of samples that would have to be taken in order to achieve the desired bound width.
def static_bound_uniform(game, z, K, alpha, subg):
    delta = alpha / (2 * game.num_strats * K) # should this use bonferroni value?
    n = game.num_strats * np.maximum(np.ceil(-16 * subg**2 * np.log(delta) / z**2).astype(int), 1)
    return n - game.num_strats


def save_data(setting, alg, name, files):
    if setting == "bandit":
        np.save("samples_" + alg + "_" + name, files[0])
        np.save("widths_" + alg + "_" + name, files[1])
        np.save("i_star_" + alg + "_" + name, files[2])
    elif setting == "finite":
        np.save("finite_" + alg + "_" + name, np.array(files))


if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2])
