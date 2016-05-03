#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an environment

set -o nounset
set -o errexit

function package() {
  ./bin/build.sh -T ${TENANT} -P ${ENVIRONMENT}

  # some manual hassling is necessary since mvn doesn't create the package we want
  ARTIFACT=comaas-${TENANT}-${ENVIRONMENT}-${GIT_HASH}.tar.gz
  echo "Creating ${ARTIFACT}"
  cd distribution/target
  tar xfz distribution-${TENANT}-${ENVIRONMENT}.tar.gz
  cd distribution
  tar cfz ${WORKDIR}/${ARTIFACT} .
  cd ${WORKDIR}

  cp ${ARTIFACT} ${DESTINATION}
  rm -rf distribution/target/distribution

  echo "Created ${DESTINATION}/${ARTIFACT}"
}

WORKDIR=$PWD
TENANT=$1
GIT_HASH=$2
ENVIRONMENT=$3
DESTINATION=$4
package