#!/bin/bash
python3 spoofing_distribution.py
echo "finished dist"
python3 regret_new.py workshop hoeffding spoofingA finite
python3 regret_new.py uniform hoeffding spoofingA finite
python3 regret_new.py UAS hoeffding spoofingA finite
python3 regret_new.py opt hoeffding spoofingA finite
python3 regret_new.py coci coci spoofingA finite
echo "finished A"
python3 regret_new.py uniform hoeffding spoofingB finite
python3 regret_new.py workshop hoeffding spoofingB finite
python3 regret_new.py opt hoeffding spoofingB finite
python3 regret_new.py UAS hoeffding spoofingB finite
python3 regret_new.py coci coci spoofingB finite
echo "finished B"
python3 regret_new.py uniform hoeffding spoofingC finite
python3 regret_new.py workshop hoeffding spoofingC finite
python3 regret_new.py opt hoeffding spoofingC finite
python3 regret_new.py UAS hoeffding spoofingC finite
python3 regret_new.py coci coci spoofingC finite
echo "finished C"
python3 regret_new.py uniform lil spoofingA finite
python3 regret_new.py workshop lil spoofingA finite
python3 regret_new.py opt lil spoofingA finite
python3 regret_new.py workshopSingle lil spoofingA finite
python3 regret_new.py workshopSingle hoeffdingSingle spoofingA finite
python3 regret_new.py UAS lil spoofingA finite
python3 regret_new.py uniform lil spoofingB finite
python3 regret_new.py workshop lil spoofingB finite
python3 regret_new.py opt lil spoofingB finite
python3 regret_new.py workshopSingle lil spoofingB finite
python3 regret_new.py workshopSingle hoeffdingSingle spoofingB finite
python3 regret_new.py UAS lil spoofingB finite
python3 regret_new.py uniform lil spoofingC finite
python3 regret_new.py workshop lil spoofingC finite
python3 regret_new.py opt lil spoofingC finite
python3 regret_new.py workshopSingle lil spoofingC finite
python3 regret_new.py workshopSingle hoeffdingSingle spoofingC finite
python3 regret_new.py UAS lil spoofingC finite
