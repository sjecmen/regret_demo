import numpy as np
import json
import SpoofingSim

def main():
    '''
    try:
        emp_min, emp_max, t = SpoofingSim.load_distribution()
    except FileNotFoundError:
        emp_min = float("inf")
        emp_max = -1 * float("inf")
        t = 0
    print("old min:", emp_min, "old max:", emp_max)
    '''
    LSHNmix = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0.7, 0, 0, 0.3]
    MSMNmix = [0, 0, 0.33, 0, 0, 0, 0, 0, 0, 0.51, 0.16, 0, 0]
    HSLNmix = [0, 0, 0.12, 0, 0, 0, 0.29, 0, 0, 0.49, 0.1, 0, 0]

    for market, mix in zip(["LSHN", "MSMN", "HSLN"], [LSHNmix, MSMNmix, HSLNmix]):
        means = np.zeros(SpoofingSim.spoofing_num_strats)
        samples = np.zeros(SpoofingSim.spoofing_num_strats)
        payoffs = np.zeros((10000, 65))
        for t in range(10000):
            strat = np.random.randint(0, SpoofingSim.spoofing_num_strats)
    
            playersList = SpoofingSim.sample_spoofing_simulation_unnorm(strat, mix, market)
            payoffList = []
            for i, playerMap in enumerate(playersList):
                payoffs[t][i] = playerMap["payoff"]
                if playerMap["strategy"] == SpoofingSim.spoofing_strategy_names[strat]:
                    payoffList.append(playerMap["payoff"])
            payoff = np.random.choice(payoffList)
            means[strat] = ((means[strat] * samples[strat]) + payoff) / (samples[strat] + 1)
            samples[strat] += 1

            if t % 1000 == 0:
                np.save("spoofing_distribution_" + market + ".npy", payoffs)
                np.save("spoofing_means_" + market + ".npy", means)
                print(t)
            t += 1

if __name__ == '__main__':
    main()
