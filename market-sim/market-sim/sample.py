#!/usr/bin/env python3
import argparse
import sys
import json
import itertools

import numpy.random as rand


def create_parser():
    parser = argparse.ArgumentParser(description="""Sample profiles from a
            mixture distribution to get expected values of outcome
            variables.""")
    parser.add_argument('--num-samples', '-n', metavar='<num-samples>',
            type=int, help="""The number of samples to gather. (default:
            infinite)""")
    parser.add_argument('--configuration', '-c',
            metavar='<configuration-file>', type=argparse.FileType('r'),
            default=sys.stdin, help="""Json file with the game configuration.
            Must have a root level field `configuration` with the game
            configuration, and a root level field `roles` that contains the
            role counts for the game. (default: stdin)""")
    parser.add_argument('--mixture', '-m', metavar='<mixture-file>',
            type=argparse.FileType('r'), default=sys.stdin, help="""Json file
            with the mixture proportions. Must be a mapping of {role:
                {strategy: probability}}. (default: stdin)""")
    parser.add_argument('--output', '-o', metavar='<output-file>',
            type=argparse.FileType('w'), default=sys.stdout, help="""File to
            write output to. Each line of the file is a new json object that
            represents a compressed game egta simulation specification file.
            (default: stdout)""")
    return parser


def main():
    args = create_parser().parse_args()
    conf = json.load(args.configuration)
    roles = conf.pop('roles')
    mix = json.load(args.mixture)
    role_info = sorted((r, roles[r]) + tuple(zip(*sorted(s.items()))) for r, s in mix.items())

    num = itertools.count()
    if args.num_samples:
        num = itertools.islice(num, args.num_samples)

    keys = {key.lower(): key for key in conf['configuration']}
    if 'randomseed' in keys:
        seed = int(conf['configuration'][keys['randomseed']])
        rand.seed(seed)
    
    try:
        for _ in num:
            samp = {role:
                    {strat: int(count) for strat, count
                     in zip(s, rand.multinomial(c, probs))
                     if count > 0}
                    for role, c, s, probs in role_info}
            conf['assignment'] = samp
            json.dump(conf, args.output, sort_keys=True)
            args.output.write('\n')

    except BrokenPipeError:
        pass


if __name__ == '__main__':
    main()
