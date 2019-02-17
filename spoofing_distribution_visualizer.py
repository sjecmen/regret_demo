import numpy as np
import matplotlib.pyplot as plt

lshn = np.load("spoofing_distribution_LSHN.npy")
msmn = np.load("spoofing_distribution_MSMN.npy")
hsln = np.load("spoofing_distribution_HSLN.npy")


def analyze(arr):
    plt.hist(arr, color='blue', edgecolor='black', bins=100)
    low = np.quantile(arr, 0.025)
    high = np.quantile(arr, 0.975)
    print(low, high)
    plt.axvline(x=low)
    plt.axvline(x=high)
    plt.show()

analyze(lshn.flatten())
analyze(msmn.flatten())
analyze(hsln.flatten())
  
