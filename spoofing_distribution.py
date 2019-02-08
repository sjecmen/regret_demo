import numpy as np
import json
import SpoofingSim

def main():
    try:
        emp_min, emp_max, t = SpoofingSim.load_distribution()
    except FileNotFoundError:
        emp_min = float("inf")
        emp_max = -1 * float("inf")
        t = 0
    print("old min:", emp_min, "old max:", emp_max)

    while t <= 100000:
        if t % 100 == 0:
            with open("spoofing_distribution.json", 'w') as past_work:
                data = {"min":emp_min, "max":emp_max, "t":t}
                json.dump(data, past_work)
            print("t:", t, "new min:", emp_min, "new max:", emp_max)

        mix = np.random.random((SpoofingSim.spoofing_num_strats))
        mix /= sum(mix)
        strat = np.random.randint(0, SpoofingSim.spoofing_num_strats)
        market = np.random.choice(["LSHN", "MSMN", "HSLN"])

        playersList = SpoofingSim.sample_spoofing_simulation_unnorm(strat, mix, market)
        for playerMap in playersList:
            emp_min = min(emp_min, playerMap["payoff"])
            emp_max = max(emp_max, playerMap["payoff"])
        t += 1

if __name__ == '__main__':
    main()
