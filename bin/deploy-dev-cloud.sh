#!/usr/bin/env bash

# Deploy the provided artifact to the dev cloud
#
# Usage: $0 <-d don't repackage/upload, just redeploy> distribution.name.tar.gz
#
# Needs to be able to resolve fileserver001.dev, and a tunnel to consul001.dev:
#
# ssh -L 4646:localhost:4646 consul001.dev -N &

set -o nounset
set -o errexit

readonly env=dev
readonly count=3

declare -A tenant_short_names=(
["ebayk"]="ek"
["mp"]="mp"
["gtau"]="au"
["mde"]="mo"
["kjca"]="ca"
)

UPLOAD=true
while getopts ":d" opt; do
  case ${opt} in
    d)
      UPLOAD=false
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
  esac
done
shift $((OPTIND-1))

if [[ $# -ne 1 ]]; then
  echo Usage: ${0} distribution_name.tar.gz
  exit 1
fi

distr_name="$1"
if [[ ! -f ${distr_name} ]]; then
  echo "${distr_name} does not exist"
fi
artifact_name=$(basename "$distr_name")
artifact_name=${artifact_name/.tar/-nomad.tar}

tenant=$(echo ${artifact_name} | cut -d- -f2)
tenant_short=${tenant_short_names[${tenant}]}
git_hash=$(git rev-parse --short HEAD)
echo "Deploying for tenant $tenant($tenant_short) with git hash $git_hash"

if [ "$UPLOAD" = true ] ; then
    dir=$(mktemp -d)

    # repackage for nomad
    # perhaps check shasum to see if upload is necessary at all / rsync?
    # get the fileserver and consul IP addresses from openstack/nova
    # openstack server list -f value -c Name -c Networks | grep fileserver001 | cut -d= -f2
    echo -n "Repackaging for Nomad... "
    tar zxf ${distr_name} -C ${dir}
    tar zcf ${dir}/${artifact_name} -C $(find ${dir} -type d -depth 1) .
    echo Done.

    echo Uploading to fileserver001.dev:
    scp ${dir}/${artifact_name} fileserver001.dev:/var/www/html/${artifact_name}
    rm -rf ${dir}
fi

echo "Posting to Nomad; evaluation ID:"
sed "s/%TENANT%/$tenant/g; s/%TENANT_SHORT%/$tenant_short/g; s/%ARTIFACT%/$artifact_name/g; s/%GIT_HASH%/$git_hash/g; s/%ENVIRONMENT%/$env/g; s/%COUNT%/$count/g" \
  bin/comaas_deploy_dev_cloud.hcl > builds/${artifact_name}.hcl
nomad run -output builds/${artifact_name}.hcl > builds/${artifact_name}.json
curl -X POST -d @builds/${artifact_name}.json http://localhost:4646/v1/jobs --header "Content-Type:application/json"
