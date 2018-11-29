import numpy as np
import matplotlib.pyplot as plt
import sys

# Creates graphs based off of saved data from regret.py

# Generates graphs for greedy, uniform, optimal, and UCB algorithms for the given setting and game.
# Parameters:
#  - setting: bandit or finite
#  - name: name of game
def main(setting, name):
    if setting == "bandit":
        output_bandit(name)
    elif setting == "finite":
        output_finite(name)


def output_bandit(name):
    samples_greedy, widths_greedy, isc_greedy = load_data("bandit", "greedy", name)
    samples_uniform, widths_uniform, isc_uniform = load_data("bandit", "uniform", name)
    samples_opt, widths_opt, isc_opt = load_data("bandit", "optimal", name)
    samples_ucb, widths_ucb, isc_ucb = load_data("bandit", "UCB", name)

    plt.rcParams.update({'font.size': 14})

    plt.plot(widths_greedy, label="SAUCB", color="green", linestyle="--")
    plt.plot(widths_uniform, label="Naive Uniform", color="red", linestyle=":")
    plt.plot(widths_opt, label="Known Best Deviation", color="blue", linestyle="-")
    plt.plot(widths_ucb, label="Modified UCB", color="gold", linestyle="-.")
    plt.xlabel("Number of samples taken")
    plt.ylabel("Width of regret bound")
    plt.legend()
    plt.savefig("widths_" + name + ".png")
    plt.show()

    T, S = samples_greedy.shape
    plt.bar(np.arange(S) + 1 - 0.3, (samples_greedy[-1] - 1)/T, width=0.2, label="SAUCB", color="green")
    plt.bar(np.arange(S) + 1 - 0.1, (samples_uniform[-1] - 1)/T, width=0.2, label="Naive Uniform", color="red")
    plt.bar(np.arange(S) + 1 + 0.1, (samples_opt[-1] - 1)/T, width=0.2, label="Known Best Deviation", color="blue")
    plt.bar(np.arange(S) + 1 + 0.3, (samples_ucb[-1] - 1)/T, width=0.2, label="Modified UCB", color="gold")
    plt.xlabel("Action index")
    plt.ylabel("Proportion of samples")
    plt.xticks(np.arange(S) + 1)
    if name == "bestInSupp":
        plt.legend(prop={'size': 12})
    else:
        plt.legend()
    plt.savefig("distribution_" + name + ".png")
    plt.show()

    plt.plot(isc_greedy, label="SAUCB", color="green", linestyle="--")
    plt.plot(isc_uniform, label="naive uniform", color="red", linestyle=":")
    plt.plot(isc_opt, label="known best deviation", color="blue", linestyle="-")
    plt.plot(isc_ucb, label="Modified UCB", color="gold", linestyle="-.")
    plt.xlabel("Number of samples taken")
    plt.ylabel("Fraction of times best deviation identified")
    plt.legend()
    plt.savefig("isc_" + name + ".png")
    plt.show()


def output_finite(name):
    samples_greedy, UB_greedy, LB_greedy = load_data("finite", "greedy", name)
    samples_uniform, UB_uniform, LB_uniform = load_data("finite", "uniform", name)
    samples_opt, UB_opt, LB_opt = load_data("finite", "optimal", name)
    samples_ucb, UB_ucb, LB_ucb = load_data("finite", "UCB", name)
    print("uniform:", samples_uniform, "/ greedy:", samples_greedy, "/ optimal:", samples_opt, "/ UCB:", samples_ucb)


def load_data(setting, alg, name):
    if setting == "bandit":
        samples = np.load("samples_" + alg + "_" + name + ".npy")
        widths = np.load("widths_" + alg + "_" + name + ".npy")
        i_star = np.load("i_star_" + alg + "_" + name + ".npy")
        return samples, widths, i_star
    elif setting == "finite":
        results = np.load("finite_" + alg + "_" + name + ".npy")
        return results


if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2])
