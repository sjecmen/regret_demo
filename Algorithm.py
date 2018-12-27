from scipy.optimize import minimize
import math
import numpy as np

class Algorithm:
    def __init__(self):
        self.alpha = 0.05

    def sample(self, means, samples, mix):
        pass

    def width(self, means, samples, mix):
        pass

    def make(name, scenario):
        if name == "workshopLIL":
            return WorkshopLIL(scenario)
        else:
            assert(False)

class WorkshopLIL(Algorithm):
    def __init__(self, scenario):
        super(WorkshopLIL, self).__init__()
        self.K = scenario.game.size()
        self.subg = scenario.game.subg()
        self.mix = scenario.mix
        self.epsilon = 0.01
        self.delta = calculate_delta_lil(self.alpha, self.K, self.epsilon)

    def bound_superarms(self, means, samples): # returns bounding term
        bound_single = np.array([lil_bound(self.epsilon, self.delta, t, self.subg) for t in samples])
        bound_sa = np.zeros((len(means)))
        for i in range(len(means)):
            coeff = np.array(self.mix)
            coeff[i] = 1 - coeff[i]
            bound_sa[i] = np.dot(coeff, bound_single)
        return bound_sa

    def derivative_bound(self, means, samples, i_star): # returns absolute value (positive)
        coeff = np.array(self.mix)
        coeff[i_star] = 1 - coeff[i_star]
        derivative = np.array([lil_derivative(self.epsilon, self.delta, samples[i], self.subg, coeff[i]) for i in range(self.K)])
        return derivative

    def width(self, means, samples):
        upper_bounds = means + self.bound_superarms(means, samples)
        lower_bounds = means - self.bound_superarms(means, samples)
        return np.max(upper_bounds) - np.max(lower_bounds)

    def sample(self, means, samples):
        bounds = means + self.bound_superarms(means, samples)
        i_star = np.argmax(bounds)
        derivatives = self.derivative_bound(means, samples, i_star)
        return np.argmax(derivatives)
        
def lil_bound(epsilon, delta, t, subg):
    t1 = 1 + math.sqrt(epsilon)
    te = (1 + epsilon) * t
    t2 = 2 * (subg**2) * ((1 + epsilon) / t) * math.log(math.log(te) / delta)
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

# Calculate the parameters for an individual lil bound, given total error alpha. Epsilon and delta
# chosen to minimize bound width of an individaul arm at time 1 subject to the constraint of alpha.
# K is number of arms.
def calculate_epsilon_delta_lil(alpha_desired, K):
    def calculate_width(x, t, subg):
        epsilon = x[0]
        delta = x[1]
        return lil_bound(epsilon, delta, t, subg)
    def calculate_alpha(x):
        epsilon = x[0]
        delta = x[1]
        t1 = (2 + epsilon) / epsilon
        t2 = delta / math.log(1 + epsilon)
        alpha_actual = K * 2 * t1 * (t2**(1 + epsilon))
        return alpha_desired - alpha_actual # non-negative if constraint satisfied
    def delta_bound(x):
        epsilon = x[0]
        delta = x[1]
        deltaUB = math.log(1 + epsilon) / math.exp(1)
        return deltaUB - delta # allows equality but shouldn't, hopefully not an issue
    x0 = np.zeros((2)) # epsilon, delta
    x0[0] = 0.5
    x0[1] = math.log(1 + 0.5) / (math.exp(1) * 2)
    fixed_args = (1, 1) # t, subg (subg is irrelevant for minimizing width, t arbitrarily fixed at 1)
    bounds = [(0, 1), (0, 1)] # allows equality but shouldn't, hopefully not an issue
    constraints = [{"type":"ineq", "fun":calculate_alpha}, {"type":"ineq", "fun":delta_bound}]
    res = minimize(calculate_width, x0, fixed_args, bounds=bounds, constraints=constraints)
    assert(res.success)
    return res.x[0], res.x[1]

# As above, but fixed epsilon
def calculate_delta_lil(alpha, K, epsilon):
    t1 = math.log(1 + epsilon)
    t2 = (epsilon * alpha) / (2 * K * (2 + epsilon))
    return t1 * (t2**(1 / (1 + epsilon)))

