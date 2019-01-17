from scipy.special import lambertw
import math
import numpy as np

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

# bound individual arm
def hoeffding_bound_single(delta, t, subg): # t is a vector
    t1 = 2 * (subg**2) * math.log(1 / delta)
    t2 = 1 / t[i] 
    return math.sqrt(t1 * t2)

# Uses the upper bound on t based only on W for all arms on both sides, since we have to union bound over time:
# t_max = t_bound_coeff * log(1/delta)
# Constraint on alpha is: alpha = t_max * K * 2 * delta
# Lambert W function used to solve log(1/delta)*delta = alpha/C.
def calculate_delta_hoeffding(t_bound_coeff, W, K, alpha):
    delta_term_coeff = t_bound_coeff * K * 2 # this * log(1/delta) * delta is the result of union bound over time and arms
    alpha_term = alpha / delta_term_coeff # this must equal log(1/delta) * delta
    log_delta = np.real(lambertw(-alpha_term, 1)) # two real solutions, larger delta requires t < 1 so use smaller

    max_t = alpha / (math.exp(log_delta) * 2 * K)
    assert(max_t >= 20)
    print("t cannot be more than", max_t)

    delta = math.exp(log_delta)
    print("delta:", delta)

    return delta


''' COCI Bound (bound from COCI paper)'''
def coci_bound(delta, samples, t, startup):
    t1 = 1 / (2 * samples)
    t2 = math.log((4 * t**3) / (startup * delta))
    return math.sqrt(t1 * t2)

# With common terms removed
def coci_derivative(coeff, samples):
    return coeff / (samples**(3/2))
