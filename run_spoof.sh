#!/bin/bash
python3 regret_new.py workshop hoeffding spoofingA finite
python3 regret_new.py SALUCB hoeffding spoofingA finite
python3 regret_new.py uniform hoeffding spoofingA finite
python3 regret_new.py workshop hoeffding spoofingB finite
python3 regret_new.py SALUCB hoeffding spoofingB finite
python3 regret_new.py uniform hoeffding spoofingB finite
python3 regret_new.py LUAS hoeffding spoofingA finite
python3 regret_new.py LUAS hoeffding spoofingB finite
python3 regret_new.py UAS hoeffding spoofingA finite
python3 regret_new.py UAS hoeffding spoofingB finite
