map(.players) | . as $x | [ range(0; .[0] | length) | [ $x[][.] ] | {role: .[0].role, strategy: .[0].strategy, payoff: (map(.payoff) | add / length)} ] | {players: .}
