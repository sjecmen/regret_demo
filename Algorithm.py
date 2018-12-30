from scipy.optimize import minimize
from scipy.special import lambertw
import math
import numpy as np

class Algorithm:
    def __init__(self, scenario, bound):
        self.bound = bound
        self.alpha = 0.05
        self.K = scenario.game.size()
        self.subg = scenario.game.subg()
        self.mix = scenario.mix
        if bound == "lil":
            self.epsilon = 0.01
            self.delta = calculate_delta_lil(self.alpha, self.K, self.epsilon)
        elif bound == "hoeffding":
            self.delta = calculate_delta_hoeffding(self.alpha, self.K, self.subg, scenario.W)
        else:
            assert(False)

    def make(name, bound, scenario):
        if name == "workshop":
            return Workshop(scenario, bound)
        elif name == "opt":
            return Opt(scenario, bound)
        elif name == "uniform":
            return Uniform(scenario, bound)
        else:
            assert(False)

    def width(self, means, samples):
        upper_bounds = means + self.bound_superarms(means, samples)
        lower_bounds = means - self.bound_superarms(means, samples)
        return np.max(upper_bounds) - np.max(lower_bounds)

    def sample(self, means, samples, mix):
        pass

    def bound_superarms(self, means, samples): # returns bounding term
        bound_sa = np.zeros((len(means)))
        if self.bound == "lil":
            bound_single = np.array([lil_bound(self.epsilon, self.delta, t, self.subg) for t in samples])
            for i in range(len(means)):
                coeff = coefficient(self.mix, i)
                bound_sa[i] = np.dot(coeff, bound_single)
        elif self.bound == "hoeffding":
            for i in range(len(means)):
                coeff = coefficient(self.mix, i)
                bound_sa[i] = hoeffding_bound(self.delta, samples, self.subg, coeff)
        else:
            assert(False)
        return bound_sa

    def derivative_bound(self, means, samples, i_star):
        coeff = coefficient(self.mix, i_star)
        if self.bound == "lil":
            derivative = np.array([lil_derivative(self.epsilon, self.delta, samples[i], self.subg, coeff[i]) for i in range(self.K)])
        elif self.bound == "hoeffding":
            derivative = np.array([hoeffding_derivative(coeff[i], samples[i]) for i in range(self.K)])
        else:
            assert(False)
        return derivative


class Workshop(Algorithm):
    def __init__(self, scenario, bound):
        super(Workshop, self).__init__(scenario, bound)

    def sample(self, means, samples):
        bounds = means + self.bound_superarms(means, samples)
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
        upper_bounds = means + self.bound_superarms(means, samples)
        lower_bounds = means - self.bound_superarms(means, samples)
        return upper_bounds[self.i_star] - lower_bounds[self.i_star]


class Uniform(Algorithm):
    def __init__(self, scenario, bound):
        super(Uniform, self).__init__(scenario, bound)

    def sample(self, means, samples):
        return np.argmin(samples)


def coefficient(mix, i_star):
    coeff = np.array(mix)
    coeff[i_star] = 1 - coeff[i_star]
    return coeff
 

''' LIL Bound '''
def lil_bound(epsilon, delta, t, subg):
    t1 = 1 + math.sqrt(epsilon)
    te = (1 + epsilon) * t
    t2 = ((2 * (subg**2) * (1 + epsilon)) / t) * math.log(math.log(te) / delta)
    return t1 * math.sqrt(t2)

def lil_derivative(epsilon, delta, t, subg, coeff):
    t1 = coeff * (1 + math.sqrt(epsilon)) * math.sqrt(2 * (subg**2) * (1 + epsilon))
    t2 = 1 / (2 * (t**(3/2)))
    t3 = math.log(math.log((1 + epsilon) * t) / delta)
    t4 = t3 - (1 / math.log((1 + epsilon) * t))
    res = t1 * t2 * t3**(-1/2) * t4
    if res < 0: # derivative is positive for a while, then becomes negative 
        print("derivative of bound positive, increase startup sample count")
        assert(False)
    return res

def calculate_delta_lil(alpha, K, epsilon):
    t1 = math.log(1 + epsilon)
    t2 = (epsilon * alpha) / (2 * K * (2 + epsilon))
    delta = t1 * (t2**(1 / (1 + epsilon)))
    print("delta:", delta)
    return delta

''' Hoeffding Bound '''
def hoeffding_bound(delta, t, subg, coeff): # t is a vector
    t1 = 2 * (subg**2) * math.log(1 / delta)
    t2 = sum([(coeff[i]**2) / t[i] for i in range(len(t))])
    return math.sqrt(t1 * t2)

# Returns absolute value (positive), with common term removed
def hoeffding_derivative(c, t): # t and c are scalars for specific term
    return c**2 / t**2

# Uses the upper bound on t based only on W for all arms on both sides, since we have to union bound over time:
# t_max = (8 * subg**2 * log(1/delta) * K)/W**2 
# Constraint on alpha is: alpha = t_max * K * 2 * delta
# Lambert W function used to solve log(1/delta)*delta = alpha/C.
def calculate_delta_hoeffding(alpha, K, subg, W):
    t_bound_coeff = (8 * K * subg**2) / (W**2) # this * log(1/delta) is the upper bound on number samples
    delta_term_coeff = t_bound_coeff * K * 2 # this * log(1/delta) * delta is the result of union bound over time and arms
    alpha_term = alpha / delta_term_coeff # this must equal log(1/delta) * delta
    log_delta = np.real(lambertw(-alpha_term, 1)) # two real solutions, larger delta requires t < 1 so use smaller

    max_t = alpha / (math.exp(log_delta) * 2 * K)
    assert(max_t >= 20)
    print("t cannot be more than", max_t)

    delta = math.exp(log_delta)
    print("delta:", delta)

    return delta
