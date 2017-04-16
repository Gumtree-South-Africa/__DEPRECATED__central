#!/bin/bash -x

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

# Filter out the tenants that are live in the cloud

if [[ "$TENANT" == "mp" || "$TENANT" == "mde" ]] ; then
    echo "Distribution not supported for $TENANT, because it's already live in the cloud"
    exit
fi

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh ${TENANT} ${GIT_HASH} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP}

for PKG in $(ls ${BUILD_DIR}/comaas-${TENANT}*); do
  PACKAGE_REGEX=".*/comaas-${TENANT}-legacy.*"
  [[ ${PKG} =~ ${PACKAGE_REGEX} ]] && continue

  `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP}
done
