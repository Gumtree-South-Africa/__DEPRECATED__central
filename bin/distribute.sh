#!/bin/bash

set -o nounset
set -o errexit

function usage() {
  echo "Usage: distribute.sh <tenant> <git_hash> <timestamp>"
  exit 1
}

# check number of args
[[ $# != 3 ]] && usage

readonly TENANT=$1
readonly GIT_HASH=$2
readonly TIMESTAMP=$3

readonly BUILD_DIR="builds"

# Filter out the tenants that are live in the cloud
if [[ "$TENANT" == "mde" ]] || [[ "$TENANT" == "mp" ]] || [[ "$TENANT" == "ebayk" ]] || [[ "$TENANT" == "gtau" ]]; then
    echo "Distribution not supported for $TENANT, because it's already live in the cloud"
    exit
fi
if [[ "$TENANT" == "it" ]]; then
    echo "Tenant $TENANT currently not supported"
    exit
fi

# Repackage into packages for the TENANT's environment
`dirname $0`/repackage.sh ${TENANT} ${GIT_HASH} ${BUILD_DIR}/comaas-${TENANT}-comaasqa-${GIT_HASH}-nomad.tar.gz ${TIMESTAMP}

for PKG in $(ls ${BUILD_DIR}/comaas-${TENANT}*); do
  PACKAGE_REGEX=".*/comaas-${TENANT}-legacy.*"
  [[ ${PKG} =~ ${PACKAGE_REGEX} ]] && continue

  `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP}
done
