{assignment: (.assignment | with_entries({key: .key, value: (reduce .value[] as $x ({}; .[$x] += 1))})), configuration: .configuration}
