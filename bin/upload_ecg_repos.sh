#!/bin/bash

set -o nounset
set -o errexit

if [ $# -ne 4 ]; then
    echo "Usage: ${0##*/} <tenant> <package> <timestamp> <environment>" 1>&2
    exit 1
fi

TENANT=$1
PACKAGE=$2
TIMESTAMP=$3
ENVIRONMENT=$4

REPO_USER="repo_comaas"
REPO_PASS="V9Knbsi4Nm"
URL="https://repositories-cloud.ecg.so/v2/files/${TENANT}/${ENVIRONMENT}/${TIMESTAMP}/${PACKAGE##*/}"
DEST_URL="http://repositories-cloud.ecg.so/platforms/comaas/files/${TENANT}/${ENVIRONMENT}/${TIMESTAMP}/${PACKAGE##*/}"

# we can't use this until cloud repos varnish cache doesn't ignore get parameters
#if curl --head -o /dev/null -svf "${DEST_URL}?$$.1"; then
#    echo "${DEST_URL} already exists!"
#    exit 1
#fi

echo "Uploading ${PACKAGE} to ${URL} as user ${REPO_USER}"
curl -s0vu ${REPO_USER}:${REPO_PASS} -X PUT -F filedata=@"${PACKAGE}" "${URL}" || exit 1

# Test if the upload succeeded
if ! curl --head -o /dev/null -svf "${DEST_URL}?$$.2"; then
    echo "Upload seems to have failed. Can't find ${DEST_URL}"
    exit 1
fi
