#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an prod and sandbox

set -o nounset
set -o errexit

function usage() {
    cat <<- EOF
    Usage: package.sh <tenant> <YYYY.mm.Jenkins_sequential_build_number>
EOF
    exit
}

[[ $# != 2 ]] && usage

TENANT="$1"
VERSION="$2"

DESTINATION="builds"
mkdir -p ${DESTINATION}
ENV=prod # the prod artifact will also be deployed to sandbox

mvn --settings etc/settings.xml versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false
bin/build.sh -T ${TENANT} -P ${ENV}
mv -v distribution/target/*.tar.gz "${DESTINATION}/comaas-${TENANT}_${VERSION}.tar.gz"
