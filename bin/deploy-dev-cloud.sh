#!/usr/bin/env bash
#
# Deploy the provided artifact to the dev cloud

set -o nounset
set -o errexit

if [[ $# -ne 1 ]]; then
  echo Usage: ${0} distribution_name.tar.gz
  exit 1
fi

distr_name="$1"
if [[ ! -f ${distr_name} ]]; then
  echo "${distr_name} does not exist"
fi

dir=$(mktemp -d)

# repackage for nomad
# perhaps check shasum to see if upload is necessary at all / rsync?
# get the fileserver and consul IP addresses from openstack/nova
echo -n "Repackaging for Nomad... "
tar zxf ${distr_name} -C ${dir}
tar zcf ${dir}/comaas.tar.gz -C $(find ${dir} -type d -depth 1) .
echo Done.

echo Uploading to fileserver001.dev:
scp ${dir}/comaas.tar.gz fileserver001.dev:/var/www/html/comaas.tar.gz
rm -rf ${dir}

echo "Posting to Nomad; evaluation ID:"
curl -f -X POST -d @bin/comaas_deploy_dev_cloud.json http://localhost:4646/v1/jobs --header "Content-Type:application/json"
