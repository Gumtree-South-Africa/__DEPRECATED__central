#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an prod and sandbox

set -o nounset
set -o errexit

function usage() {
    cat <<- EOF
    Usage: package.sh <tenant> <git hash> <timestamp>
EOF
    exit
}

function parseArgs() {
    [[ $# == 0 ]] && usage

    TENANT="$1"
    GIT_HASH="$2"
    TIMESTAMP="$3"

    DESTINATION="builds"
    mkdir -p ${DESTINATION}
}

function createCloudPackage() {
    ARTIFACT_NAME="comaas-${TENANT}_${1}-${TIMESTAMP}-${GIT_HASH}"
    rm -rf distribution/target/distribution/conf
    mkdir -p distribution/target/distribution/conf
    cp distribution/conf/${TENANT}/${1}/* distribution/target/distribution/conf
    chmod 0775 distribution/target/distribution/log
    cp -r distribution/target/distribution distribution/target/${ARTIFACT_NAME}
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/ ${ARTIFACT_NAME}
    rm -rf distribution/target/${ARTIFACT_NAME}
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"
}

function package() {
    # Create the artifact with the prod properties. This will be used as a base package to create packages from for other Comaas environments.
    # Note that we are unpacking the prod package and repackaging it below, this is because it gets packaged by maven with
    # basedir distribution/, where the deploy.py script expects comaas-${TENANT}_${ENV}-${TIMESTAMP}-${GIT_HASH}
    bin/build.sh -T ${TENANT} -P prod
    tar xf distribution/target/distribution-${TENANT}-prod.tar.gz -C distribution/target

    # Create a zip with all the tenant's configuration to be imported into Consul
    ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
    tar zcf ${DESTINATION}/${ARTIFACT_NAME} -C distribution/conf/${TENANT} ./import_into_consul
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}"

    # Now create a package with cloud sandbox and prod properties that will be deployed using deploy.py
    createCloudPackage sandbox
    createCloudPackage prod

    # This is needed to save disk space on the builder nodes
    rm -rf distribution/target/distribution
}

parseArgs "$@"
package
