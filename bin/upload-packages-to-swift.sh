#! /bin/sh

set +x # leave this in, since this file contains a password
set -o nounset
set -o errexit

if [ $# -ne 3 ]; then
    echo "Usage: ${0##*/} <tenant> <git_hash> <timestamp>" 1>&2
    exit 1
fi

TENANT=$1
GIT_HASH=$2
TIMESTAMP=$3

function swift() {
    docker run --rm \
        -v ${PWD}/builds:/objects \
        -e OS_AUTH_URL=https://keystone.dus1.cloud.ecg.so/v2.0 \
        -e OS_TENANT_NAME="comaas-qa" \
        -e OS_USERNAME="comaas-control-swift" \
        -e OS_PASSWORD="1307zENI8tN56r22U4X@" \
        -e OS_PROJECT_NAME="comaas-control-prod" \
        -e OS_REGION_NAME="dus1" \
        registry.ecg.so/mp-so/python-swiftclient:latest \
        swift $@
}

# Remove a previous build of the same git hash
function delete_folder() {
    files=$(swift list comaas --prefix ${TENANT}/$1/${GIT_HASH})
    if [ ! -z "${files}" ]; then
        echo "Deleting previous build ${TENANT}/$1/${GIT_HASH}"
        swift delete comaas ${files}
    fi
}

function join_by {
    local IFS="$1"; shift; echo "$*";
}

# Remove old builds
function clean_old_builds() {
    BUILDS_TO_KEEP=${2}
    hashes_to_keep=$(git log -${BUILDS_TO_KEEP} --pretty=format:"%h")
    joined=$(join_by '|' ${hashes_to_keep})
    files=$(swift list comaas --prefix ${TENANT}/$1 | grep -vE "($joined)")
    if [ ! -z "${files}" ]; then
        echo "Deleting old builds"
        swift delete comaas ${files}
    fi
}

function upload_it() {
    swift upload --changed --object-name ${TENANT}/$2/${GIT_HASH}/$1 comaas /objects/$1
}

clean_old_builds sandbox 5
clean_old_builds prod 10

delete_folder sandbox
delete_folder prod

# Nomad packages:
#upload_it comaas-${TENANT}-comaasqa-${GIT_HASH}-nomad.tar.gz qa
#upload_it comaas-${TENANT}-sandbox-${GIT_HASH}-nomad.tar.gz sandbox

# upload the configuration zip
ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
upload_it ${ARTIFACT_NAME} sandbox
upload_it ${ARTIFACT_NAME} prod

# deploy.py package:
upload_it comaas-${TENANT}_sandbox-${TIMESTAMP}-${GIT_HASH}.tar.gz sandbox
if [[ -d distribution/conf/${TENANT}/prod ]]; then
    upload_it comaas-${TENANT}_prod-${TIMESTAMP}-${GIT_HASH}.tar.gz prod
fi
