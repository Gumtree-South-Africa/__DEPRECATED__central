#! /bin/sh

# This is a temporary way of releasing comaas on deploy.py based hosts. Remove once we're deploying to Nomad.
# This is a "manual" script, that just automates a number of steps.

if [ "$#" -ne 2 ]; then
  echo "Usage: $0 tenant git_hash"
  exit 1
fi

readonly TENANT=$1
readonly NEW_HASH=$2
declare -A TENANTS=(
  ["mp"]="mp"
  ["ebayk"]="ek"
  ["mde"]="mo"
  ["kjca"]="ca"
  ["gtau"]="au"
  ["gtuk"]="uk"
)
declare -A ENVS=(
  ["lp"]="sandbox"
  ["prod"]="prod"
)
readonly ENV=${ENVS[$ENVIRONMENT]}
readonly CUR_HASH=$(curl -s ${TENANTS[${TENANT}]}-core001:8080/health | jq .version | cut -d\" -f2)

echo "Post this to the #ecg-comaas-$TENANT channel in Slack:"
echo
echo "We're going to release Comaas for $TENANT version \`$NEW_HASH\` to $ENV in $LOCATION. Current version on $ENV is \`$CUR_HASH\`."
echo "The diff can be found here: https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/compare/$CUR_HASH...$NEW_HASH"
echo
read -rsp $'Press any key to continue...\n' -n1 key
echo
echo -n "Getting releases... "
get_releases.py http://repositories-cloud.ecg.so/ --username=repo_comaas --password=V9Knbsi4Nm --path=/${TENANT}/${ENV} --target=/opt/tarballs/ecg-comaas/ --platform=comaas --hash=${NEW_HASH}
echo "done."
cd $(find /opt/tarballs/ecg-comaas/ -name \*${NEW_HASH}\* | head -1 | xargs dirname)

readonly FILE_COUNT=$(ls -lash *${NEW_HASH}*.tar.gz | wc -l)
if [[ $FILE_COUNT -lt 2 ]]; then
  echo "Required files not found in ${PWD}. Was this artifact deployed to ecg.repositories.so? Check Jenkins: https://buildmaster.ams1.cloud.ops.qa.comaas.ecg.so/job/comaas_upload_to_repos/"
  exit 1
fi

echo "Loading properties into Consul..."
properties-to-consul -file *configuration*.tar.gz
echo "done."

echo "Starting deploy..."
deploy.py --config /etc/comaas/comaas-${TENANT}.yaml --logdir ~ --component comaas-${TENANT}_${ENV}-*.tar.gz
echo "done."

echo "Post this to the #ecg-comaas-$TENANT channel in Slack:"
echo
echo "We've successfully deployed Comaas for ${TENANT} version \`${NEW_HASH}\` to ${ENV}."
echo
