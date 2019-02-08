#!/bin/bash
python3 regret_new.py workshop hoeffding spoofingA finite
python3 regret_new.py uniform hoeffding spoofingA finite
python3 regret_new.py opt hoeffding spoofingA finite
echo "finished A"
python3 regret_new.py workshop hoeffding spoofingB finite
python3 regret_new.py uniform hoeffding spoofingB finite
python3 regret_new.py opt hoeffding spoofingB finite
echo "finished B"
python3 regret_new.py UAS hoeffding spoofingA finite
python3 regret_new.py UAS hoeffding spoofingB finite
python3 regret_new.py coci coci spoofingA finite
python3 regret_new.py coci coci spoofingB finite
python3 regret_new.py uniform hoeffding spoofingC finite
python3 regret_new.py workshop hoeffding spoofingC finite
python3 regret_new.py opt hoeffding spoofingC finite
python3 regret_new.py UAS hoeffding spoofingC finite
python3 regret_new.py coci coci spoofingC finite
echo "finished C"
