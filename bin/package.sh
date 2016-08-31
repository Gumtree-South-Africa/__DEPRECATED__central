#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an environment

set -o nounset
set -o errexit

function usage() {
    cat <<- EOF
    Usage: package.sh <tenant> <git hash> <timestamp> <build dir:'builds'>
EOF
    exit
}

function parseArgs() {
    [[ $# == 0 ]] && usage

    TENANT="$1"
    GIT_HASH="$2"
    TIMESTAMP="$3"

    if [ $# -eq 4 ]; then
        DESTINATION=${4}
    else
        DESTINATION="builds"
    fi
    mkdir -p ${DESTINATION}
}

function package() {
    # Create the artifact with the comaasqa properties. This will be used for a Nomad deploy on comaasqa
    # and for repackaging and distribution to the tenants' legacy environments.
    ./bin/build.sh -T ${TENANT} -P comaasqa

    # Some manual hassling is necessary since mvn doesn't create the package we want
    ARTIFACT_NAME="comaas-${TENANT}-comaasqa-${TIMESTAMP}-${GIT_HASH}.tar.gz"
    echo "Creating ${ARTIFACT_NAME}"
    tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target
    tar cf ${DESTINATION}/${ARTIFACT_NAME} -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}"

    # Now create a package with cloud sandbox properties that will be deployed using deploy.py
    ARTIFACT_NAME="comaas-${TENANT}-sandbox-${TIMESTAMP}-${GIT_HASH}.tar.gz"
    echo "Creating ${ARTIFACT_NAME}"
    rm -f distribution/target/distribution/conf/*
    cp distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf
    mv distribution/target/distribution distribution/target/comaas-${TENANT}-${TIMESTAMP}-${GIT_HASH}
    tar cf ${DESTINATION}/${ARTIFACT_NAME} -C distribution/target/ comaas-${TENANT}-${TIMESTAMP}-${GIT_HASH}
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}"

    rm -rf distribution/target/comaas-${TENANT}-${TIMESTAMP}-${GIT_HASH}

    # Insert here: package for cloud prod, similar to sandbox above
}

parseArgs "$@"
package
