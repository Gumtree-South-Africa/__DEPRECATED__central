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
    ARTIFACT_NAME="comaas-${TENANT}-comaasqa-${TIMESTAMP}-${GIT_HASH}"
    echo "Creating ${ARTIFACT_NAME}.tar.gz"
    tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    # Now create a package with cloud sandbox properties that will be deployed using deploy.py
    ARTIFACT_NAME="comaas-${TENANT}_sandbox-${TIMESTAMP}-${GIT_HASH}"
    echo "Creating ${ARTIFACT_NAME}.tar.gz"
    rm -f distribution/target/distribution/conf
    mkdir distribution/target/distribution/conf-${TENANT}
    cp distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf-${TENANT}
    sed -i "s~/conf ~/conf-${TENANT} ~" distribution/target/distribution/bin/comaas
    mv distribution/target/distribution distribution/target/${ARTIFACT_NAME}
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/ ${ARTIFACT_NAME}
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    rm -rf distribution/target/${ARTIFACT_NAME}

    # Insert here: package for cloud prod, similar to sandbox above
}

parseArgs "$@"
package
