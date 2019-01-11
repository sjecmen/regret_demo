import numpy as np
import json
import SpoofingSim

def main():
    try:
        emp_min, emp_max, t = SpoofingSim.load_distribution()
    except FileNotFoundError:
        mix = np.random.random((SpoofingSim.spoofing_num_strats))
        mix /= sum(mix)
        payoffs = SpoofingSim.sample_spoofing_simulation_unnorm(mix)
        emp_min = np.min(payoffs)
        emp_max = np.max(payoffs)
        t = 1
    print("old min:", emp_min, "old max:", emp_max)

    while True:
        if t % 100 == 0:
            with open("spoofing_distribution.json", 'w') as past_work:
                data = {"min":emp_min, "max":emp_max, "t":t}
                json.dump(data, past_work)
            print("t:", t, "new min:", emp_min, "new max:", emp_max)

        mix = np.random.random((SpoofingSim.spoofing_num_strats))
        mix /= sum(mix)
        strat = np.random.randint(0, SpoofingSim.spoofing_num_strats)

        playersList = SpoofingSim.sample_spoofing_simulation_unnorm(strat, mix)
        payoffList = []
        for playerMap in playersList:
            payoffList.append(playerMap["payoff"])
        emp_min = min(emp_min, min(payoffList))
        emp_max = max(emp_max, max(payoffList))
        t += 1

if __name__ == '__main__':
    main()
