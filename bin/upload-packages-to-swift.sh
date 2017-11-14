#! /bin/sh

set +x # leave this in, since this file contains a password
set -o nounset
set -o errexit

if [ $# -ne 2 ]; then
    echo "Usage: ${0##*/} <tenant> <YYYY.mm.Jenkins_sequential_build_number>" 1>&2
    exit 1
fi

TENANT=$1
VERSION=$2

function swift() {
    docker run --rm \
        -v ${PWD}/builds:/objects \
        -e OS_AUTH_URL=https://keystone.dus1.cloud.ecg.so/v2.0 \
        -e OS_TENANT_NAME="comaas-qa" \
        -e OS_USERNAME=${OS_USERNAME} \
        -e OS_PASSWORD=${OS_PASSWORD} \
        -e OS_PROJECT_NAME="comaas-control-prod" \
        -e OS_REGION_NAME="dus1" \
        registry.ecg.so/mp-so/python-swiftclient:latest \
        swift $@
}

# Remove a previous build of the same git hash
#function delete_folder() {
#    files=$(swift list comaas --prefix ${TENANT}/$1/${GIT_HASH})
#    if [ ! -z "${files}" ]; then
#        echo "Deleting previous build ${TENANT}/$1/${GIT_HASH}"
#        swift delete comaas ${files}
#        echo "Done"
#    fi
#}

function join_by {
    local IFS="$1"; shift; echo "$*";
}

# Remove old builds
#function clean_old_builds() {
#    BUILDS_TO_KEEP=${2}
#    master_hashes_to_keep=$(git log -${BUILDS_TO_KEEP} --pretty=format:"%h" origin/master)
#    # also keep builds from other branches
#    branch_hashes_to_keep=$(git log -${BUILDS_TO_KEEP} --branches --pretty=format:"%h" origin/master)
#    joined=$(join_by '|' ${master_hashes_to_keep} ${branch_hashes_to_keep})
#    set +o errexit
#    files=$(swift list comaas --prefix ${TENANT}/$1 | grep --invert-match --extended-regexp "($joined)")
#    set -o errexit
#    if [ ! -z "${files}" ]; then
#        echo "Deleting old builds"
#        swift delete comaas ${files}
#        echo "Done"
#    fi
#}

function upload_it() {
    local TARGET="${TENANT}/builds/${VERSION}/$1"
    echo "Uploading build ${1} to ${TARGET}"
    swift upload --changed --object-name ${TARGET} comaas /objects/$1
    echo "Done"
}

# Rough size calculation: (nr of tenants) * (environments) * (size of artifact) * (desired build history)
# Currently 7 * 1 * 130 * 100 ~= 91 gb
#clean_old_builds prod 100

#delete_folder prod

upload_it comaas-${TENANT}_prod-${VERSION}.tar.gz
