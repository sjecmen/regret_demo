import math
import numpy as np
import Bounds as bd

class Algorithm:
    def __init__(self, scenario, bound):
        self.bound = bound
        self.alpha = 0.05
        self.K = scenario.game.size()
        self.subg = scenario.game.subg()
        self.mix = scenario.mix
        self.startup = 1
        if bound == "lil":
            self.epsilon = 0.01
            self.delta = bd.calculate_delta_lil(self.alpha, self.K, self.epsilon)
            self.startup = 2
        elif bound == "hoeffding":
            t_bound_coeff = (16 * self.K * self.subg**2) / (scenario.W**2) # use uniform bound
            self.delta = bd.calculate_delta_hoeffding(t_bound_coeff, scenario.W, self.K, self.alpha)
        elif bound == "coci": # requires bounded in [0,1]
            assert(scenario.game.isBounded())
            self.delta = self.alpha # probability of error is self.alpha
        elif bound == "hoeffdingSingle": # super-arm bounds constructed from single arm bounds
            t_bound_coeff = (32 * self.K * self.subg**2) / (scenario.W**2) # uniform again
            self.delta = bd.calculate_delta_hoeffding(t_bound_coeff, scenario.W, self.K, self.alpha)
        else:
            assert(False)

    def make(name, bound, scenario):
        if name == "workshop":
            return Workshop(scenario, bound)
        elif name == "opt":
            return Opt(scenario, bound)
        elif name == "uniform":
            return Uniform(scenario, bound)
        elif name == "coci":
            return COCI(scenario, bound)
        elif name == "workshopSingle":
            return WorkshopSingleArm(scenario, bound)
        elif name == "UAS":
            return UAS(scenario, bound)
        else:
            assert(False)

    def width(self, means, samples):
        sa_means = means - np.dot(self.mix, means)        
        bounds = self.bound_superarms(means, samples) 
        upper_bounds = sa_means + bounds
        lower_bounds = sa_means - bounds
        return np.max(upper_bounds) - np.max(lower_bounds) # could take max of 0 on lower bound as well, since regret cannot be negative - not necc.

    def sample(self, means, samples):
        assert(False)
        pass

    def bound_superarms(self, means, samples): # returns bounding term
        if self.bound == "lil" or self.bound == "coci" or self.bound == "hoeffdingSingle":
            bound_single = self.bound_individual_arms(means, samples)
            bound_sa = calculate_sa_bounds(self.mix, bound_single)
        elif self.bound == "hoeffding":
            bound_sa = np.zeros((len(means)))
            for i in range(len(means)):
                coeff = coefficient(self.mix, i)
                bound_sa[i] = bd.hoeffding_bound(self.delta, samples, self.subg, coeff)
        else:
            assert(False)
        return bound_sa

    def bound_individual_arms(self, means, samples): # used only in special algorithms
        if self.bound == "lil":
            bound_single = np.array([bd.lil_bound(self.epsilon, self.delta, t, self.subg) for t in samples])
        elif self.bound == "hoeffding":
            assert(False)
        elif self.bound == "coci": # assumes rewards bounded on [0,1]
            bound_single = np.array([bd.coci_bound(self.delta, sample, sum(samples), self.startup) for sample in samples])
        elif self.bound == "hoeffdingSingle":
            bound_single = np.array([bd.hoeffding_bound_single(self.delta, t, self.subg) for t in samples])
        else:
            assert(False)
        return bound_single

    def derivative_bound(self, means, samples, i_star):
        coeff = coefficient(self.mix, i_star)
        if self.bound == "lil":
            derivative = np.array([bd.lil_derivative(self.epsilon, self.delta, samples[i], self.subg, coeff[i]) for i in range(self.K)])
        elif self.bound == "hoeffding":
            derivative = np.array([bd.hoeffding_derivative(coeff[i], samples[i]) for i in range(self.K)])
        elif self.bound == "coci":
            derivative = np.array([bd.coci_derivative(coeff[i], samples[i], self.delta, sum(samples), self.startup) for i in range(self.K)])
        elif self.bound == "hoeffdingSingle":
            derivative = np.array([bd.hoeffding_derivative_single(coeff[i], samples[i]) for i in range(self.K)])
        else:
            assert(False)
        return derivative

    def opt(self):
        return False


class Workshop(Algorithm):
    def __init__(self, scenario, bound):
        super(Workshop, self).__init__(scenario, bound)
        self.i_stars = np.zeros(self.K) # TODO

    def sample(self, means, samples):
        sa_means = means - np.dot(self.mix, means)
        bounds = sa_means + self.bound_superarms(means, samples)
        i_star = np.random.randint(0, 2)#np.argmax(bounds)
        self.i_stars[i_star] += 1 # TODO
        #print("bounds", [round(bound, 4) for bound in bounds])
        derivatives = self.derivative_bound(means, samples, i_star)
        return np.argmax(derivatives)


class WorkshopSingleArm(Algorithm):
    def __init__(self, scenario, bound):
        super(WorkshopSingleArm, self).__init__(scenario, bound)

    def sample(self, means, samples):
        bounds = means + self.bound_individual_arms(means, samples)
        i_star = np.argmax(bounds)
        derivatives = self.derivative_bound(means, samples, i_star)
        return np.argmax(derivatives)


class Opt(Algorithm):
    def __init__(self, scenario, bound):
        super(Opt, self).__init__(scenario, bound)
        self.i_star = scenario.game.i_star()

    def sample(self, means, samples):
        derivatives = self.derivative_bound(means, samples, self.i_star)
        return np.argmax(derivatives)

    def width(self, means, samples): # override to use our i_star
        sa_means = means - np.dot(self.mix, means)
        bounds = self.bound_superarms(means, samples)
        upper_bounds = sa_means + bounds
        lower_bounds = sa_means - bounds
        return upper_bounds[self.i_star] - lower_bounds[self.i_star]

    def opt(self):
        return True


class Uniform(Algorithm):
    def __init__(self, scenario, bound):
        super(Uniform, self).__init__(scenario, bound)

    def sample(self, means, samples):
        return np.argmin(samples)


class UAS(Algorithm): # sample to get to uniform distribution among for i_star
    def __init__(self, scenario, bound):
        super(UAS, self).__init__(scenario, bound)

    def sample(self, means, samples):
        sa_means = means - np.dot(self.mix, means)
        bounds = sa_means + self.bound_superarms(means, samples)
        i_star = np.argmax(bounds)
        candidates = [samples[i] if i == i_star or self.mix[i] > 0 else float("inf") for i in range(self.K)]
        return np.argmin(candidates)


class COCI(Algorithm):
    def __init__(self, scenario, bound):
        super(COCI, self).__init__(scenario, bound)
        assert(scenario.game.isBounded())
        assert(self.bound == "coci")

    def sample(self, means, samples):
        bounding_terms = self.bound_individual_arms(means, samples)
        upper_bounds = means + bounding_terms
        lower_bounds = means - bounding_terms
        candidates = [] # any arm where we're uncertain about coefficient
        for i in range(self.K):
            is_best_arm = all([j == i or lower_bounds[i] > upper_bounds[j] for j in range(len(upper_bounds))]) # we are certain i is best
            is_not_best_arm = any([j != i and upper_bounds[i] < lower_bounds[j] for j in range(len(upper_bounds))]) # we are certain i is not best
            assert(not (is_best_arm and is_not_best_arm))
            if not is_best_arm and not is_not_best_arm: # uncertain
                candidates.append(i)
            elif is_best_arm:
                best_arm = i

        if len(candidates) == 0: # sample optimal distribution for best_arm we know
            derivative = self.derivative_bound(means, samples, best_arm)
            return np.argmax(derivative)
        else: # sample candidate with largest radius
            mask = np.ones(len(bounding_terms), dtype=bool)
            mask[candidates] = False
            bounding_terms[mask] = 0 # set non-candidate radius to 0
            return np.argmax(bounding_terms)


def coefficient(mix, i_star):
    coeff = np.array(mix)
    coeff[i_star] = 1 - coeff[i_star]
    return coeff

# Return array of values weighted by coefficients for each position
# IMPORTANT: Do not use this to calculate regret! Regret has weights 1-p_i, -p_j; this calculates 1-p_i, p_j.
#     Instead, use this for calculating the bounding terms for superarms from the individual bounds.
def calculate_sa_bounds(mix, bounds):
    sv = np.zeros((len(bounds)))
    for i in range(len(bounds)):
        coeff = coefficient(mix, i)
        sv[i] = np.dot(coeff, bounds)
    return sv
