import numpy as np
import matplotlib.pyplot as plt
import sys

# Creates graphs based off of saved data from regret.py

fontsize = 18
legendsize = 18
ymax = 2
xmax = 10000
xstep = 5000
xbuffer = 100

# Generates graphs for greedy, uniform, optimal, and UCB algorithms for the given setting and game.
# Parameters:
#  - setting: bandit or finite
#  - name: name of game
def main(name, which, num):
    if which == "A":
        output_workshop_comp(name, num)
    elif which == "B":
        output_main_comp(name, num)
    elif which == "C":
        output_empirical_comp(name, num)
    elif which == "D":
        output_samples(name, num)


def output_workshop_comp(name, num):
    samples_greedy, widths_greedy, correct_greedy, _ = load_data("bandit", "workshop", name, "hoeffding", num)
    samples_greedyL, widths_greedyL, correct_greedyL, _ = load_data("bandit", "workshop", name, "lil", num)
    samples_greedySingle, widths_greedySingle, correct_greedySingle, _ = load_data("bandit", "workshopSingle", name, "hoeffdingSingle", num)
    samples_greedySingleL, widths_greedySingleL, correct_greedySingleL, _ = load_data("bandit", "workshopSingle", name, "lil", num)

    plt.rcParams.update({'font.size': fontsize})

    plt.plot(widths_greedySingleL, label="single-lil-SAUCB", color="cyan", linestyle=":")
    plt.plot(widths_greedySingle, label="single-SAUCB", color="magenta", linestyle="-.")
    plt.plot(widths_greedyL, label="lil-SAUCB", color="blue", linestyle="--")
    plt.plot(widths_greedy, label="SAUCB", color="green", linestyle="-")
    plt.axis([-xbuffer, xmax+xbuffer, -0.1, ymax+0.1])
    plt.xlabel("Number of samples taken")
    plt.ylabel("Width of regret bound")
    ax = plt.gca()
    h, l = ax.get_legend_handles_labels()
    ax.legend(h[::-1], l[::-1], fontsize=legendsize)
    plt.xticks(np.arange(0, xmax+1, xstep))
    plt.tight_layout()
    plt.savefig("widths_" + name + "_A.png")
    plt.show()
   
    plt.plot(correct_greedySingleL, label="single-lil-SAUCB", color="cyan", linestyle=":")
    plt.plot(correct_greedySingle, label="single-SAUCB", color="magenta", linestyle="-.")
    plt.plot(correct_greedyL, label="lil-SAUCB", color="blue", linestyle="--")
    plt.plot(correct_greedy, label="SAUCB", color="green", linestyle="-")
    plt.axis([-xbuffer, xmax+xbuffer, -0.03, 1.03])
    plt.xlabel("Number of samples taken")
    plt.ylabel("Probability of correct bound")
    ax = plt.gca()
    h, l = ax.get_legend_handles_labels()
    ax.legend(h[::-1], l[::-1], fontsize=legendsize)
    plt.tight_layout()
    plt.savefig("correct_" + name + "_A.png")
    plt.show()


def output_main_comp(name, num):
    samples_greedy, widths_greedy, correct_greedy, _ = load_data("bandit", "workshop", name, "hoeffding", num)
    samples_uniform, widths_uniform, correct_uniform, _ = load_data("bandit", "uniform", name, "hoeffding", num)
    samples_ucb, widths_ucb, correct_ucb, _ = load_data("bandit", "coci", name, "hoeffdingSingle", num)
    samples_uas, widths_uas, correct_uas, _ = load_data("bandit", "UAS", name, "hoeffding", num)
    samples_se, widths_se, correct_se, _ = load_data("bandit", "SE", name, "hoeffdingSingle", num)
    print(widths_se[5000])

    plt.rcParams.update({'font.size': fontsize})

    plt.plot(widths_ucb, label="COCI", color="black", linestyle=":")
    plt.plot(widths_uas, label="UAS", color="gold", linestyle="-.")
    plt.plot(widths_uniform, label="Naive Uniform", color="red", linestyle="--")
    plt.plot(widths_greedy, label="SAUCB", color="green", linestyle="-")
    plt.plot(widths_se, label="Modified SE", color="blue", linestyle="-")
    plt.axis([-xbuffer, xmax+xbuffer, -0.1, ymax+0.1])
    plt.xlabel("Number of samples taken")
    plt.ylabel("Width of regret bound")
    ax = plt.gca()
    h, l = ax.get_legend_handles_labels()
    ax.legend(h[::-1], l[::-1], fontsize=legendsize)
    plt.xticks(np.arange(0, xmax+1, xstep))
    plt.tight_layout()
    plt.savefig("widths_" + name + "_B.png")
    plt.show()

    plt.plot(correct_ucb, label="COCI", color="black", linestyle=":")
    plt.plot(correct_uas, label="UAS", color="gold", linestyle="-.")
    plt.plot(correct_uniform, label="Naive Uniform", color="red", linestyle="--")
    plt.plot(correct_greedy, label="SAUCB", color="green", linestyle="-")
    plt.plot(correct_se, label="Modified SE", color="blue", linestyle="-")
    xl, xu, yl, yu = plt.axis()
    plt.axis([-xbuffer, xmax+xbuffer, -0.03, 1.03])
    plt.xlabel("Number of samples taken")
    plt.ylabel("Probability of correct bound")
    ax = plt.gca()
    h, l = ax.get_legend_handles_labels()
    ax.legend(h[::-1], l[::-1], fontsize=legendsize)
    plt.tight_layout()
    plt.savefig("correct_" + name + "_B.png")
    plt.show()



def output_empirical_comp(name, num):
    samples_greedy, widths_greedy, correct_greedy, t_greedy = load_data("finite", "workshop", name, "hoeffding", num)
    samples_uniform, widths_uniform, correct_uniform, t_uniform = load_data("finite", "uniform", name, "hoeffding", num)
    samples_uas, widths_uas, correct_uas, t_uas = load_data("finite", "UAS", name, "hoeffding", num)

    plt.rcParams.update({'font.size': fontsize})

    plt.plot(widths_uas, label="UAS", color="gold", linestyle="-.")
    plt.plot(widths_uniform, label="Naive Uniform", color="red", linestyle="--")
    plt.plot(widths_greedy, label="SAUCB", color="green", linestyle="-")
    plt.axvline(x=t_uas, color="gold", linestyle=":")
    plt.axvline(x=t_uniform, color = "red", linestyle=":")
    plt.axvline(x=t_greedy, color = "green", linestyle=":")
    plt.xlabel("Number of samples taken")
    plt.ylabel("Width of regret bound")
    ax = plt.gca()
    h, l = ax.get_legend_handles_labels()
    ax.legend(h[::-1], l[::-1], fontsize=legendsize)
    xl, xu, yl, yu = plt.axis()
    plt.axis([xl, xu, -0.1, ymax+0.1])
    plt.tight_layout()
    plt.savefig("widths_" + name + "_C.png")
    plt.show()

    T, S = samples_greedy.shape
    plt.bar(np.arange(S) + 1 - 0.2, (samples_greedy[-1] )/T, width=0.2, label="SAUCB", color="green")
    plt.bar(np.arange(S) + 1 + 0.0, (samples_uniform[-1] )/T, width=0.2, label="Naive Uniform", color="red")
    plt.bar(np.arange(S) + 1 + 0.2, (samples_uas[-1] )/T, width=0.2, label="UAS", color="gold")
    plt.xlabel("Arm index")
    plt.ylabel("Proportion of samples")
    plt.xticks(np.arange(S) + 1)
    plt.legend(fontsize=14)
    plt.tight_layout()
    plt.savefig("distribution_" + name + "_C.png")
    plt.show()
    

def output_samples(name, num):
    legendsize = 14

    samples_greedy, widths_greedy, correct_greedy, _ = load_data("bandit", "workshop", name, "hoeffding", num)
    samples_greedyL, widths_greedyL, correct_greedyL, _ = load_data("bandit", "workshop", name, "lil", num)
    samples_greedySingle, widths_greedySingle, correct_greedySingle, _ = load_data("bandit", "workshopSingle", name, "hoeffdingSingle", num)
    samples_greedySingleL, widths_greedySingleL, correct_greedySingleL, _ = load_data("bandit", "workshopSingle", name, "lil", num)
    samples_uniform, widths_uniform, correct_uniform, _ = load_data("bandit", "uniform", name, "hoeffding", num)
    samples_ucb, widths_ucb, correct_ucb, _ = load_data("bandit", "coci", name, "hoeffdingSingle", num)
    samples_uas, widths_uas, correct_uas, _ = load_data("bandit", "UAS", name, "hoeffding", num)
    samples_se, widths_se, correct_se, _ = load_data("bandit", "SE", name, "hoeffdingSingle", num)


    plt.rcParams.update({'font.size': fontsize})

    T, S = samples_greedy.shape
    w = 0.12
    plt.bar(np.arange(S) + 1 - (3*w), (samples_greedy[-1] )/T, width=w, label="SAUCB", color="green")
    plt.bar(np.arange(S) + 1 - (2*w), (samples_uniform[-1])/T, width=w, label="Naive Uniform", color="red")
    plt.bar(np.arange(S) + 1 - w, (samples_uas[-1] )/T, width=w, label="UAS", color="gold")
    plt.bar(np.arange(S) + 1 + 0.0, (samples_ucb[-1] )/T, width=w, label="COCI", color="black")
    plt.bar(np.arange(S) + 1 + w, (samples_greedyL[-1] )/T, width=w, label="lil-SAUCB", color="blue")
    plt.bar(np.arange(S) + 1 + (2*w), (samples_greedySingle[-1] )/T, width=w, label="single-SAUCB", color="magenta")
    plt.bar(np.arange(S) + 1 + (3*w), (samples_greedySingleL[-1] )/T, width=w, label="single-lil-SAUCB", color="cyan")
    plt.bar(np.arange(S) + 1 + (4*w), (samples_se[-1] )/T, width=w, label="Modified SE", color="blue")
    plt.xlabel("Action index")
    plt.ylabel("Proportion of samples")
    plt.xticks(np.arange(S) + 1, fontsize=12)
    plt.legend(fontsize=legendsize)
    plt.tight_layout()
    plt.savefig("distribution_" + name + ".png")
    plt.show()


def load_data(setting, alg, name, bound, num = None):
    samples = np.load("samples_" + alg + "_" + name + "_" + bound + "_" + setting + ".npy")
    widths = np.load("widths_" + alg + "_" + name + "_" + bound + "_" + setting + ".npy")
    correct = np.load("correct_" + alg + "_" + name + "_" + bound + "_" + setting + ".npy")

    final = np.argmax(widths < 0.05)
    print(alg, bound, ":", final)
    total = final if final > 0 else 50000
    return samples[:total], widths[:total], correct[:total], final

def load_extra_data(setting, alg, name, bound, num):
    UB = np.load("UB_" + alg + "_" + name + "_" + bound + "_" + setting + ".npy")
    means = np.load("mean_" + alg + "_" + name + "_" + bound + "_" + setting + ".npy")
    return UB, means


if __name__ == '__main__':
    if (len(sys.argv) == 3):
        main(sys.argv[1], sys.argv[2], None)
    else:
        main(sys.argv[1], sys.argv[2], int(sys.argv[3]))

