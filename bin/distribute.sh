#!/bin/bash

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: distribute.sh <tenant> <git_hash> <artifact> <timestamp> <build_dir>
EOF
  exit
}

# check amount of args
[[ $# == 0 ]] && usage

readonly TENANT=$1
readonly GIT_HASH=$2
readonly ARTIFACT_NAME=$3
readonly TIMESTAMP=$4
readonly BUILD_DIR=$5

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh ${TENANT} ${GIT_HASH} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP}

case "${TENANT}" in
  "mp")
    readonly MP_PACKAGE_REGEX=".*/nl.marktplaats.mp-replyts2_comaas-([0-9a-zA-Z]+)-.*"

    # Upload or deploy (only the 3 most recent packages)
    for PKG in $(ls -tdr ${BUILD_DIR}/nl.marktplaats.mp-replyts2* | tail -n3); do

      if [[ ${PKG} =~ ${MP_PACKAGE_REGEX} ]]; then
        DESTINATION="${BASH_REMATCH[1]}"
      else
        DESTINATION=""
      fi

      if [[ "$DESTINATION" == "comaasqa" || "$DESTINATION" == "local" ]] ; then
        continue
      fi

      # Deploy for mp demo, upload for all other environments/tenants
      if [[ "$DESTINATION" == "demo" ]] ; then
        # This requires the deploy.py script to be on the PATH
        deploy.py --redeploy --config distribution/conf/mp/demo/deploy.conf --logdir . --component ${PKG} --ignore-lb
      else
        `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} ${DESTINATION}
      fi
    done

    ;;
  *)
    for PKG in $(ls ${BUILD_DIR}/comaas-${TENANT}*); do
      PACKAGE_REGEX=".*/comaas-${TENANT}-(comaasqa|local).*"
      [[ ${PKG} =~ ${PACKAGE_REGEX} ]] && continue

      # UPLOAD configuration
      CONFIG_REGEX=".*/comaas-${TENANT}-configuration.*"
      if [[ ${PKG} =~ ${CONFIG_REGEX} ]]; then
        `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} sandbox
#        `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} prod
        continue
      fi

      # UPLOAD SANDBOX PACKAGE
      SANDBOX_REGEX=".*/comaas-${TENANT}_sandbox.*"
      if [[ ${PKG} =~ ${SANDBOX_REGEX} ]]; then
        `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} sandbox
        continue
      fi

      `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP}
      if [[ "${TENANT}" == "mde" ]]; then
        `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} prod
      fi
    done

    ;;
esac
