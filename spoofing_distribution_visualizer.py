import numpy as np
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab

lshn = np.load("spoofing_distribution_LSHN.npy")
msmn = np.load("spoofing_distribution_MSMN.npy")
hsln = np.load("spoofing_distribution_HSLN.npy")

lshn_samples = np.load("spoofing_samples_LSHN.npy")
msmn_samples = np.load("spoofing_samples_MSMN.npy")
hsln_samples = np.load("spoofing_samples_HSLN.npy")

lshn_bystrat = np.load("spoofing_bystrat_LSHN.npy")
msmn_bystrat = np.load("spoofing_bystrat_MSMN.npy")
hsln_bystrat = np.load("spoofing_bystrat_HSLN.npy")

LSHNmix = np.load("spoofing_mix_LSHN.npy")#[0, 0, 0, 0, 0, 0, 0, 0, 0, 0.7, 0, 0, 0.3]
MSMNmix = np.load("spoofing_mix_MSMN.npy")#[0, 0, 0.33, 0, 0, 0, 0, 0, 0, 0.51, 0.16, 0, 0]
HSLNmix = np.load("spoofing_mix_HSLN.npy")#[0, 0, 0.12, 0, 0, 0, 0.29, 0, 0, 0.49, 0.1, 0, 0]

def analyze(arr, samples, bystrat, mix):
    plt.hist(arr, color='blue', edgecolor='black', bins=100, normed=True)
    low = np.percentile(arr, 25)
    high = np.percentile(arr, 75)
    plt.axvline(x=low)
    plt.axvline(x=high)

    means = np.zeros(len(samples))
    for strat in range(len(samples)):
        s = int(samples[strat])
        stuff = bystrat[strat][:s]
        normed = (stuff - low) / (high - low)
        normed[normed > 1] = 1
        normed[normed < 0] = 0
        mean = np.mean(normed)
        means[strat] = mean
    print(mix)
    sa_means = means - np.dot(mix, means)
    print(sa_means)
    #plt.show()

print("LSHN")
analyze(lshn.flatten(), lshn_samples, lshn_bystrat, LSHNmix)
print(lshn.flatten().size)
print("MSMN")
analyze(msmn.flatten(), msmn_samples, msmn_bystrat, MSMNmix)
print("HSLN")
analyze(hsln.flatten(), hsln_samples, hsln_bystrat, HSLNmix)
  
