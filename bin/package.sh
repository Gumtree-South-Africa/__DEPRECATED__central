#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an environment

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
    sed -i'.bak' 's~-DlogDir="\$BASEDIR"/log~-DlogDir="/opt/replyts/logs"~' distribution/target/distribution/bin/comaas
    cp -r distribution/target/distribution distribution/target/${ARTIFACT_NAME}
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/ ${ARTIFACT_NAME}
    rm -rf distribution/target/${ARTIFACT_NAME}
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"
}

function package() {
    # Create the artifact with the comaas-qa properties. This will be used as a base package to create
    # packages from for other Comaas environments, as well as for repackaging and distribution to
    # the tenants' legacy environments.
    ./bin/build.sh -T ${TENANT} -P comaasqa
    tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target

    # Create a zip with all the tenant's configuration to be imported into Consul
    ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
    tar zcf ${DESTINATION}/${ARTIFACT_NAME} -C distribution/conf/${TENANT} ./import_into_consul
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}"

    # Remove the root directory from the comaasqa package for Nomad
    ARTIFACT_NAME="comaas-${TENANT}-comaasqa-${GIT_HASH}-nomad"
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    # Remove the root directory from the sandbox package for Nomad and add config
    ARTIFACT_NAME="comaas-${TENANT}-sandbox-${GIT_HASH}-nomad"
    cp distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf
    tar czf ${DESTINATION}/${ARTIFACT_NAME}.tar.gz -C distribution/target/distribution .
    echo "Created ${DESTINATION}/${ARTIFACT_NAME}.tar.gz"

    # hardcode the logDir to /opt/replyts/logs for deploy.py packages
    sed -i'.bak' 's~-DlogDir="\$BASEDIR"/log~-DlogDir="/opt/replyts/logs"~' distribution/target/distribution/bin/comaas

    # Now create a package with cloud sandbox properties that will be deployed using deploy.py
    createCloudPackage sandbox

    # Now create a package with cloud prod properties that will be deployed using deploy.py
    createCloudPackage prod

    # This is needed to save disk space on the builder nodes
    rm -rf distribution/target/distribution
}

parseArgs "$@"
package
