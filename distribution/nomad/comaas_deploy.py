#!/usr/bin/env python3
"""
Deploy COMaaS on Nomad

This script does three things:
1. Add properties to Consul's KV store
2. Put the <count> parameter in the job spec
3. POST the job spec to Nomad

Prerequisites:
- A Nomad job spec in HCL
- An artifact on a web server reachable by the core nodes
- A properties file

Notes:
- It does not need the downloaded artifact, because the core nodes will download it from the
  repository themselves.
- It does not do any progress or health checking, this is left up to a separate script/Nagios
  check. This should look in Consul to see if the correct number of nodes with the correct version
  are healthy within time T.

Usage:
    comaas_deploy.py --properties properties --jobspec jobspec --count count [--api api]

Options:
    --properties=props  Properties file to import into Consul
    --jobspec=jobspec   Location of the Nomad job spec template file
    --count=count       Number of nodes to deploy to
    --api=api           The Nomad node to post to [default: http://consul001:4646]
"""

from os.path import isfile
from subprocess import check_output, CalledProcessError
from re import compile as re_compile
from time import strftime
from docopt import docopt


COUNT_PATTERN = re_compile(r'\s*count\s*=\s*["\']*\d+["\']*')


def check_parameters(arguments):
    """ Check validity of some of the parameters """
    params_ok = True

    if not isfile(arguments['--properties']):
        print("--properties file cannot be found")
        params_ok = False

    if not isfile(arguments['--jobspec']):
        print("--jobspec file cannot be found")
        params_ok = False

    count = arguments['--count']
    if not count.isdecimal():
        print("--count should be a positive int")
        params_ok = False

    if not params_ok:
        exit(1)

    api = arguments['--api']
    if api is None:
        api = 'http://consul001:4646'

    return api, count


def post_to_nomad(api, jobspec):
    """ POST job to Nomad """
    cmd = "NOMAD_ADDR={} nomad run {}".format(api, jobspec)
    try:
        print(check_output(cmd, shell=True).decode("utf-8"))
    except CalledProcessError as err:
        print("Nomad exit code:", err.returncode)
        print(err.output.decode("utf-8"))


def replace_count(jobspec_file, count):
    """ Replace count in the job spec with desired number """
    out_filename = jobspec_file.replace(".hcl",
                                        "-deployed-at-{}.hcl".format(strftime("%Y%m%d-%H%M%S")))
    print("Writing {} as backup job spec".format(out_filename))

    with open(jobspec_file, mode='r', encoding='utf-8') as in_file, open(out_filename, mode='w', encoding='utf-8') as out_file:
        for line in in_file:
            if COUNT_PATTERN.match(line):
                out_file.write("count = {}".format(count))
            else:
                out_file.write(line)
    return out_filename


def main(args):
    """ main """
    api, count = check_parameters(args)
    jobspec_to_deploy = replace_count(args['--jobspec'], count)
    post_to_nomad(api, jobspec_to_deploy)


if __name__ == '__main__':
    main(docopt(__doc__))
