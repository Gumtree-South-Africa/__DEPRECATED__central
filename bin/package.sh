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

    # Create a zip with all the tenant's configuration to be imported into Consul
    ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
    tar zcf ${DESTINATION}/${ARTIFACT_NAME} -C distribution/conf/${TENANT} ./import_into_consul
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}"

    # Remove the root directory from the comaasqa package for Nomad
    ARTIFACT_NAME="comaas-${TENANT}-comaasqa-${TIMESTAMP}-${GIT_HASH}-nomad"
    tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    # Remove the root directory from the sandbox package for Nomad and add config
    ARTIFACT_NAME="comaas-${TENANT}-sandbox-${TIMESTAMP}-${GIT_HASH}-nomad"
    cp -v distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    # Now create a package with cloud sandbox properties that will be deployed using deploy.py
    ARTIFACT_NAME="comaas-${TENANT}_sandbox-${TIMESTAMP}-${GIT_HASH}"
    rm -rf distribution/target/distribution/conf
    mkdir distribution/target/distribution/conf
    cp distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf
    mv distribution/target/distribution distribution/target/${ARTIFACT_NAME}
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/ ${ARTIFACT_NAME}
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    rm -rf distribution/target/${ARTIFACT_NAME}

    # Insert here: package for cloud prod, similar to sandbox above
}

parseArgs "$@"
package
