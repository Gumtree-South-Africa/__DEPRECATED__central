#!/bin/bash

set -o nounset
set -o errexit

if [ $# -ne 4 ]; then
    echo "Usage: ${0##*/} <tenant> <package> <timestamp> <environment>" 1>&2
    exit 1
fi

TENANT=$1
GIT_HASH=$2
TIMESTAMP=$3
BUILD_DIR=$4


# Nomad packages:
./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/comaas-${TENANT}-comaasqa-${TIMESTAMP}-${GIT_HASH}-nomad.tar.gz ${TIMESTAMP} sandbox
./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/comaas-${TENANT}-sandbox-${TIMESTAMP}-${GIT_HASH}-nomad.tar.gz ${TIMESTAMP} sandbox


# properties:

# Create a zip with all the tenant's configuration to be imported into Consul
ARTIFACT_NAME="comaas-${TENANT}-configuration-${TIMESTAMP}-${GIT_HASH}.tar.gz"
tar zcf ${BUILD_DIR}/${ARTIFACT_NAME} -C distribution/conf/${TENANT} ./import_into_consul
echo "Created ${BUILD_DIR}/${ARTIFACT_NAME}"

# upload
./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP} sandbox
./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP} prod


# sandbox 'deployer' package:

# unpack the comaasqa package
tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target

# Create a package for sandbox
PACKAGE_NAME="comaas-${TENANT}_sandbox-${TIMESTAMP}-${GIT_HASH}"
ARTIFACT_NAME="${PACKAGE_NAME}.tar.gz"
rm -rf distribution/target/distribution/conf
mkdir -p distribution/target/distribution/conf-${TENANT}
cp distribution/conf/${TENANT}/sandbox/* distribution/target/distribution/conf-${TENANT}
sed -i.bak "s~/conf ~/conf-${TENANT} ~" distribution/target/distribution/bin/comaas
mv distribution/target/distribution distribution/target/${PACKAGE_NAME}
tar czf ${BUILD_DIR}/${ARTIFACT_NAME} -C distribution/target/ ${PACKAGE_NAME}
rm -rf distribution/target/${PACKAGE_NAME}
echo "Created ${BUILD_DIR}/${ARTIFACT_NAME}"

# upload
./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP} sandbox


# production 'deployer' package:

if [[ -d distribution/conf/${TENANT}/prod ]]; then
    # unpack the comaasqa package
    tar xf distribution/target/distribution-${TENANT}-comaasqa.tar.gz -C distribution/target
      
    # Create a package for prod
    PACKAGE_NAME="comaas-${TENANT}_prod-${TIMESTAMP}-${GIT_HASH}"
    ARTIFACT_NAME="${PACKAGE_NAME}.tar.gz"
    rm -rf distribution/target/distribution/conf
    mkdir -p distribution/target/distribution/conf-${TENANT}
    cp distribution/conf/${TENANT}/prod/* distribution/target/distribution/conf-${TENANT}
    sed -i.bak "s~/conf ~/conf-${TENANT} ~" distribution/target/distribution/bin/comaas
    chmod 775 distribution/target/distribution/log
    mv distribution/target/distribution distribution/target/${PACKAGE_NAME}
    tar czf ${BUILD_DIR}/${ARTIFACT_NAME} -C distribution/target/ ${PACKAGE_NAME}
    rm -rf distribution/target/${PACKAGE_NAME}
    echo "Created ${BUILD_DIR}/${ARTIFACT_NAME}"

    # upload
    ./bin/upload_ecg_repos.sh ${TENANT} ${BUILD_DIR}/${ARTIFACT_NAME} ${TIMESTAMP} prod
fi
