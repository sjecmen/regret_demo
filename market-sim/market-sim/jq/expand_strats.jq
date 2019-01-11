.assignment = (.assignment // {} | with_entries( {key: .key, value: (.value | to_entries | map(.key as $strat | range(0; .value) | $strat))} ) // {})
