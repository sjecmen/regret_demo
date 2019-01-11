.players | group_by(.role) | map({(.[0].role): (group_by(.strategy) | map({(.[0].strategy): (map(.payoff) | add / length) }) | add) }) | add
