#!/bin/bash
python regret_new.py uniform lil boundedC bandit
python regret_new.py uniform hoeffding boundedC bandit
python regret_new.py workshop lil boundedC bandit
python regret_new.py workshop hoeffding boundedC bandit
python regret_new.py opt lil boundedC bandit
python regret_new.py opt hoeffding boundedC bandit
python regret_new.py coci coci boundedC bandit
echo "finished C"
python regret_new.py uniform lil boundedB bandit
python regret_new.py uniform hoeffding boundedB bandit
python regret_new.py workshop lil boundedB bandit
python regret_new.py workshop hoeffding boundedB bandit
python regret_new.py opt lil boundedB bandit
python regret_new.py opt hoeffding boundedB bandit
python regret_new.py coci coci boundedB bandit
echo "finished B"
python regret_new.py uniform lil boundedA bandit
python regret_new.py uniform hoeffding boundedA bandit
python regret_new.py workshop lil boundedA bandit
python regret_new.py workshop hoeffding boundedA bandit
python regret_new.py opt lil boundedA bandit
python regret_new.py opt hoeffding boundedA bandit
python regret_new.py coci coci boundedA bandit
echo "finished A"
