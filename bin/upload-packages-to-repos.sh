#!/bin/bash

set -o nounset
set -o errexit

if [ $# -ne 3 ]; then
    echo "Usage: ${0##*/} <tenant> <git_hash> <timestamp>" 1>&2
    exit 1
fi

TENANT=$1
GIT_HASH=$2
TIMESTAMP=$3

BUILD_DIR="builds"

function upload_it() {
    ./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/${1} ${GIT_HASH} ${2}
}

# Nomad packages:
upload_it comaas-${TENANT}-comaasqa-${GIT_HASH}-nomad.tar.gz qa
upload_it comaas-${TENANT}-sandbox-${GIT_HASH}-nomad.tar.gz sandbox

# upload the configuration zip
ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
upload_it ${ARTIFACT_NAME} sandbox
upload_it ${ARTIFACT_NAME} prod

# sandbox 'deployer' package:
upload_it comaas-${TENANT}_sandbox-${TIMESTAMP}-${GIT_HASH}.tar.gz sandbox

# production 'deployer' package:
if [[ -d distribution/conf/${TENANT}/prod ]]; then
    upload_it comaas-${TENANT}_prod-${TIMESTAMP}-${GIT_HASH}.tar.gz prod
fi
