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
    samples_greedy, widths_greedy = load_data("bandit", "workshop", name)
    samples_uniform, widths_uniform = load_data("bandit", "uniform", name)
    samples_opt, widths_opt = load_data("bandit", "opt", name)
    samples_ucb, widths_ucb = load_data("bandit", "coci", name)

    plt.rcParams.update({'font.size': 14})

    plt.plot(widths_greedy, label="SAUCB", color="green", linestyle="--")
    plt.plot(widths_uniform, label="Naive Uniform", color="red", linestyle=":")
    plt.plot(widths_opt, label="Known Best Deviation", color="blue", linestyle="-")
    plt.plot(widths_ucb, label="COCI", color="gold", linestyle="-.")
    plt.xlabel("Number of samples taken")
    plt.ylabel("Width of regret bound")
    plt.legend()
    plt.savefig("widths_" + name + ".png")
    plt.show()

    T, S = samples_greedy.shape
    plt.bar(np.arange(S) + 1 - 0.3, (samples_greedy[-1] - 1)/T, width=0.2, label="SAUCB", color="green")
    plt.bar(np.arange(S) + 1 - 0.1, (samples_uniform[-1] - 1)/T, width=0.2, label="Naive Uniform", color="red")
    plt.bar(np.arange(S) + 1 + 0.1, (samples_opt[-1] - 1)/T, width=0.2, label="Known Best Deviation", color="blue")
    plt.bar(np.arange(S) + 1 + 0.3, (samples_ucb[-1] - 1)/T, width=0.2, label="COCI", color="gold")
    plt.xlabel("Action index")
    plt.ylabel("Proportion of samples")
    plt.xticks(np.arange(S) + 1)
    if name == "bestInSupp":
        plt.legend(prop={'size': 12})
    else:
        plt.legend()
    plt.savefig("distribution_" + name + ".png")
    plt.show()


def output_finite(name):
    samples_greedy, _ = load_data("finite", "workshop", name)
    samples_uniform, _ = load_data("finite", "uniform", name)
    samples_opt, _ = load_data("finite", "optimal", name)
    samples_ucb, _ = load_data("finite", "coci", name)
    print("uniform:", sum(samples_uniform[-1]), "/ workshop:", sum(samples_greedy[-1]), "/ optimal:", sum(samples_opt[-1]), "/ COCI:", sum(samples_ucb[-1]))


def load_data(setting, alg, name):
    samples = np.load("samples_" + alg + "_" + name + "_" + setting + ".npy")
    widths = np.load("widths_" + alg + "_" + name + "_" + setting + ".npy")
    return samples, widths

if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2])
