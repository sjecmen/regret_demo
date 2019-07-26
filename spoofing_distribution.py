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
    LSHNmix = random_mix() #[0, 0, 0, 0, 0, 0, 0, 0, 0, 0.7, 0, 0, 0.3]
    MSMNmix = random_mix() #[0, 0, 0.33, 0, 0, 0, 0, 0, 0, 0.51, 0.16, 0, 0]
    HSLNmix = random_mix() #[0, 0, 0.12, 0, 0, 0, 0.29, 0, 0, 0.49, 0.1, 0, 0]

    for market, mix in zip(["LSHN", "MSMN", "HSLN"], [LSHNmix, MSMNmix, HSLNmix]):
        bystrat = np.zeros((SpoofingSim.spoofing_num_strats, 1000))
        samples = np.zeros(SpoofingSim.spoofing_num_strats)
        payoffs = np.zeros((1000, 65))
        for t in range(1000):
            strat = np.random.randint(0, SpoofingSim.spoofing_num_strats)
    
            playersList = SpoofingSim.sample_spoofing_simulation_unnorm(strat, mix, market)
            payoffList = []
            for i, playerMap in enumerate(playersList):
                payoffs[t][i] = playerMap["payoff"]
                if playerMap["strategy"] == SpoofingSim.spoofing_strategy_names[strat]:
                    payoffList.append(playerMap["payoff"])
            payoff = np.random.choice(payoffList)
            bystrat[strat][int(samples[strat])] = payoff
            samples[strat] += 1
            t += 1
            if t % 100 == 0:
                print(t)

        np.save("spoofing_distribution_" + market + ".npy", payoffs)
        np.save("spoofing_bystrat_" + market + ".npy", bystrat)
        np.save("spoofing_samples_" + market + ".npy", samples)
        np.save("spoofing_mix_" + market + ".npy", mix)

def random_mix():
    m = np.random.random(13)
    m /= sum(m)
    print(m)
    return m


if __name__ == '__main__':
    main()
