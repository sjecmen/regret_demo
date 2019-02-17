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

    for market in ["LSHN", "MSMN", "HSLN"]:
        payoffs = np.zeros((10000, 65))
        for t in range(10000):
            mix = np.random.random((SpoofingSim.spoofing_num_strats))
            mix /= sum(mix)
            strat = np.random.randint(0, SpoofingSim.spoofing_num_strats)
            #market = np.random.choice(["LSHN", "MSMN", "HSLN"])
    
            playersList = SpoofingSim.sample_spoofing_simulation_unnorm(strat, mix, market)
            for i, playerMap in enumerate(playersList):
                payoffs[t][i] = playerMap["payoff"]
            if t % 1000 == 0:
                np.save("spoofing_distribution_" + market + ".npy", payoffs)
                print(t)
            t += 1

if __name__ == '__main__':
    main()
