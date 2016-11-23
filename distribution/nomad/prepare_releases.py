#!/usr/bin/env python3
"""
Prepare COMaaS releases
This script prepares COMaaS releases. After running, you will have a number of directories, each
with a Nomad job spec file and a release.sh script that will run that release.

This script does five things:
1. Download list of releases, then for each release:
2. Gets Nomad job spec template from GitHub
3. Fills in the templates parameters
4. Downloads the properties file that needs to be imported into Consul from GitHub
5. Write a deploy.sh helper script

Prerequisites:
- A Nomad job spec in json where COUNT, GIT_HASH, ARTIFACT, TENANT, TENANT_SHORT, ENVIRONMENT, and
  CHECKSUM will be populated by correct values
- An artifact on a web server reachable by the core nodes
- A properties file
- TODO A 256 bit sha checksum (shasum -a 256)

Notes:
- You will want to run this script periodically

Usage:
    prepare_releases.py --hostname=<hostname> --env=<env> --tenant=<tenant> --username=<username> --password=<password> --gh_username=<github_username> --gh_password=<github_password> [--target=<target>] [--limit=limit]

Options:
    --hostname=<hostname>               Host to download from [default: https://repositories.ecg.so]
    --env=<env>                         The environment to prepare for (sandbox | qa | prod)
    --tenant=<tenant>                   The tenant to prepare for (mp | kjca | ebayk ...)
    --username=<username>               Username to download from repository server
    --password=<password>               Password to download from repository server
    --gh_username=<github_username>     Username to download from github
    --gh_password=<github_password>     Password for download from github
    --target=<target>                   Target directory to download to [default: .]
    --limit=<limit>                     Number of releases to prepare [default: 2]
"""

from time import strftime
from os.path import dirname, exists, join
from os import mkdir, chmod
from re import compile as re_compile
from docopt import docopt
import requests


# Filenames will be "kjca/sandbox/20161125-1713/comaas-kjca_sandbox-20161125-1713-15b763d.tar.gz"
FILENAME_RE = r'\w+/\w+/(\d{8}-\d{4})/comaas-[a-z]+-[a-z]+-\d{8}-\d{4}-([a-f0-9]{7})-nomad.tar.gz'
FILENAME_PATTERN = re_compile(FILENAME_RE)

SCRIPT_TEMPLATE = """#! /usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: deploy.sh count"
    echo Where count is the number of nodes that should run this version.
    echo
    exit 1
fi

readonly base=$(cd $(dirname $0) ; pwd)
if [[ ! -f "${base}/%PROPERTIES_FILE%" ]]; then
    echo Incomplete release, properties file ${base}/%PROPERTIES_FILE% is missing.
    exit 1
fi
if [[ ! -f "${base}/%JOBSPEC%" ]]; then
    echo Incomplete release, job spec file ${base}/%JOBSPEC% is missing.
    exit 1
fi

echo About to release COMaaS.
echo
echo "Tenant:      %TENANT%"
echo "Environment: %ENVIRONMENT%"
echo "Githash:     %GITHASH%"
echo "Build date:  %BUILD_DATE%"
echo "Count:       ${1}"
echo
echo "Using job spec and properties from directory ${base}"
echo
read -p "Are you sure? " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "OK here we go!"
    comaas_deploy.py --properties=${base}/%PROPERTIES_FILE% --jobspec=${base}/%JOBSPEC% --count ${1}
else
    echo "Not doing anything."
fi
echo
"""

TENANT_SHORT_NAMES = {
    "ebayk": "ek",
    "mp": "mp",
    "gtau": "au",
    "mde": "mo",
    "kjca": "ca",
}
ALLOWED_ENVIRONMENTS = ['dev', 'qa', 'sandbox', 'prod']

# Guessing the count for now, will be replaced at deploy time
COUNT = '3'


def recursive_mkdir(path):
    """ mkdir -p, from http://stackoverflow.com/a/18503387 """
    sub_path = dirname(path)
    if not exists(sub_path):
        recursive_mkdir(sub_path)
    if not exists(path):
        mkdir(path)


def get_releases(list_url, auth, limit, tenant, env):
    """ Get a list of releases from the repositories server """
    filedict = requests.get(list_url, auth=auth).json()

    # This can be removed once we have one single artifact per tenant
    filedict = {filedict[path]['mtime']: path[1:]
                for path in filedict
                if "comaas-{}-{}".format(tenant, env) in path and "-nomad.tar.gz" in path}

    keys_list = sorted(filedict.keys())
    filedict = {k: filedict[k] for k in keys_list[-limit:]}

    return filedict


def check_parameters(arguments):
    """ Check validity of some of the parameters """
    params_ok = True
    if not arguments['--tenant'] in TENANT_SHORT_NAMES:
        print("--tenant should be in list of (" + " | ".join(TENANT_SHORT_NAMES.keys()) + ")")
        params_ok = False

    if not arguments['--env'] in ALLOWED_ENVIRONMENTS:
        print("--env should be in list of (" + " | ".join(ALLOWED_ENVIRONMENTS) + ")")
        params_ok = False

    if not params_ok:
        exit(1)

    hostname = arguments.get('--hostname')
    if hostname.endswith('/'):
        hostname = hostname[:-1]

    if arguments.get('--limit') == 'none':
        limit = None
    else:
        limit = int(arguments.get('--limit'))

    target = arguments.get('--target')
    if target is None:
        target = '.'

    return hostname, limit, target


def populate_template(tenant, artifact, githash, checksum, environment, jobspec, parameterized_job_spec_filename):
    """ Populate Nomad job spec with release and environment specific values """
    with open(parameterized_job_spec_filename, mode='w', encoding='utf-8') as out_file:
        out_file.write(
            jobspec.replace("%TENANT%", tenant)
            .replace("%TENANT_SHORT%", TENANT_SHORT_NAMES[tenant])
            .replace("%ARTIFACT%", artifact)
            .replace("%GIT_HASH%", githash)
            .replace("%CHECKSUM%", checksum)
            .replace("%ENVIRONMENT%", environment)
            .replace("%COUNT%", COUNT)
        )


def download_from_github(githash, path, auth):
    """ Download file from the ecg-comaas-central repo on github """
    url_tpl = "https://raw.github.corp.ebay.com/ecg-comaas/ecg-comaas-central/{}/{}"
    url = url_tpl.format(githash, path)
    response = requests.get(url, auth=auth)
    if response.status_code == requests.codes.ok:
        return response.text
    else:
        print('Could not download from GitHub: {} - {} - {}'.format(githash, path, url))
        exit(1)


def main(args):
    """ main program flow """

    # Check parameters
    hostname, limit, target = check_parameters(args)
    tenant = args.get('--tenant')
    env = args.get('--env')

    list_url = "{}/v2/files/{}/{}".format(hostname, tenant, env)
    download_url = "{}/platforms/comaas/files".format(hostname)

    releases = get_releases(list_url, (args.get('--username'), args.get('--password')),
                            limit, tenant, env)

    for _, release in releases.items():
        build_date, githash = FILENAME_PATTERN.match(release).groups()
        release_path = join(target, 'release-{}-{}-{}'.format(tenant, build_date, githash))
        print("Preparing release for {} {} {} in {}".format(tenant, build_date,
                                                            githash, release_path))

        recursive_mkdir(release_path)

        artifact = "{}/{}".format(download_url, release)

        jobspec_filename = "jobspec-{}-{}-{}-{}-{}.hcl".format(build_date, githash, tenant,
                                                               env, strftime("%Y%m%d-%H%M%S"))
        jobspec_target = join(release_path, jobspec_filename)

        # Get the jobspec template from github. For now, not all releases will have the template, so
        # we're falling back to master here. Remove and set to download from 'githash' as soon as
        # we don't need any releases that are older than 20161128.
        githash_master = 'master'
        jobspec_tpl = download_from_github(githash_master,
                                           "distribution/nomad/comaas_deploy_cloud.hcl",
                                           (args.get('--gh_username'), args.get('--gh_password')))

        checksum = "TODO-checksum"
        populate_template(tenant, artifact, githash, checksum, env, jobspec_tpl, jobspec_target)

        props_file = 'distribution/conf/{}/import_into_consul/{}.properties'.format(tenant, env)
        properties = download_from_github(githash, props_file,
                                          (args.get('--gh_username'), args.get('--gh_password')))

        props_file_saved = '{}-{}-{}-{}.properties'.format(tenant, env, build_date, githash)
        with open(join(release_path, props_file_saved),
                  mode='w', encoding='utf-8') as out_f:
            out_f.write(properties)

        deploy_file = join(release_path, 'deploy.sh')
        with open(deploy_file, mode='w', encoding='utf-8') as out_f:
            out_f.write(
                SCRIPT_TEMPLATE
                .replace("%TENANT%", tenant)
                .replace("%GITHASH%", githash)
                .replace("%JOBSPEC%", jobspec_filename)
                .replace("%PROPERTIES_FILE%", props_file_saved)
                .replace("%BUILD_DATE%", build_date)
                .replace("%ENVIRONMENT%", env)
            )
            chmod(deploy_file, 0o755)


if __name__ == '__main__':
    main(docopt(__doc__))
