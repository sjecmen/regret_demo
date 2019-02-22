import numpy as np
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab

lshn = np.load("spoofing_distribution_LSHN.npy")
msmn = np.load("spoofing_distribution_MSMN.npy")
hsln = np.load("spoofing_distribution_HSLN.npy")

lshn_means = np.load("spoofing_means_LSHN.npy")
msmn_means = np.load("spoofing_means_MSMN.npy")
hsln_means = np.load("spoofing_means_HSLN.npy")


def analyze(arr, means):
    plt.hist(arr, color='blue', edgecolor='black', bins=100, normed=True)
    low = np.percentile(arr, 25)
    high = np.percentile(arr, 75)
    mean = np.mean(arr)
    g = np.sqrt(np.var(arr))
    print(low, high)
    #print(mean - g, mean + g)
    #newmeans = ((means - low) / (high - low))
    #newmeans[newmeans > 1] = 1
    #newmeans[newmeans < 0] = 0
    plt.axvline(x=low)
    plt.axvline(x=high)
    #x = np.linspace(min(arr), max(arr), 100)
    #plt.plot(x, mlab.normpdf(x, mean, g))
    plt.show()

analyze(lshn.flatten(), lshn_means)
analyze(msmn.flatten(), msmn_means)
analyze(hsln.flatten(), hsln_means)
  
