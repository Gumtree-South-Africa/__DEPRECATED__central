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
if [[ "$TENANT" == "mde" ]] ; then
    echo "Distribution not supported for $TENANT, because it's already live in the cloud"
    exit
fi

# Repackage into packages for the TENANT's environment
`dirname $0`/repackage.sh ${TENANT} ${GIT_HASH} ${BUILD_DIR}/comaas-${TENANT}-comaasqa-${GIT_HASH}-nomad.tar.gz ${TIMESTAMP}

if [[ "$TENANT" == "mp" ]] ; then
	readonly MP_PACKAGE_REGEX=".*/nl.marktplaats.mp-replyts2_comaas-([0-9a-zA-Z]+)-.*"

	# Upload or deploy (only the 3 most recent packages)
	for PKG in $(ls -tdr ${BUILD_DIR}/nl.marktplaats.mp-replyts2* | tail -n3); do

		if [[ ${PKG} =~ ${MP_PACKAGE_REGEX} ]]; then
			DESTINATION="${BASH_REMATCH[1]}"
		else
			DESTINATION=""
		fi

		`dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP} ${DESTINATION}
	done
	exit
fi

for PKG in $(ls ${BUILD_DIR}/comaas-${TENANT}*); do
  PACKAGE_REGEX=".*/comaas-${TENANT}-legacy.*"
  [[ ${PKG} =~ ${PACKAGE_REGEX} ]] && continue

  `dirname $0`/upload.sh ${TENANT} ${GIT_HASH} ${PKG} ${TIMESTAMP}
done
