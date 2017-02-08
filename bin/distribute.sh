#!/bin/bash

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: distribute.sh <tenant> <git_hash> <artifact> <timestamp>
EOF
  exit
}

# check amount of args
[[ $# == 0 ]] && usage

readonly TENANT=$1
readonly GIT_HASH=$2
readonly ARTIFACT_NAME=$3
readonly TIMESTAMP=$4

readonly BUILD_DIR="builds"

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh ${TENANT} ${GIT_HASH} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP}

if [[ "$TENANT" == "mp" ]] ; then
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

  `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} ${DESTINATION}
  done
fi

for PKG in $(ls ${BUILD_DIR}/comaas-${TENANT}*); do
  PACKAGE_REGEX=".*/comaas-${TENANT}-(comaasqa|local|sandbox|prod|configuration).*"
  [[ ${PKG} =~ ${PACKAGE_REGEX} ]] && continue

  `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP}
  if [[ "${TENANT}" == "mde" ]]; then
    `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} prod
  fi
done
