#!/usr/bin/env bash
#
# package.sh creates a comaas .tar.gz for a tenant for an environment

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: package.sh <tenant> <environment> <artifact_name> <builddir:'builds'>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  WORKDIR=$PWD
  TENANT=$1
  ENVIRONMENT=$2
  ARTIFACT_NAME=$3

  if [ $# -eq 4 ]; then
    DESTINATION=$4
  else
    DESTINATION=builds/
  fi
  mkdir -p ${DESTINATION}
}

function package() {
  ./bin/build.sh -T ${TENANT} -P ${ENVIRONMENT}

  # some manual hassling is necessary since mvn doesn't create the package we want
  echo "Creating ${ARTIFACT_NAME}"
  cd distribution/target
  tar xfz distribution-${TENANT}-${ENVIRONMENT}.tar.gz
  cd distribution
  tar cfz ${WORKDIR}/${ARTIFACT_NAME} .
  cd ${WORKDIR}

  mv ${ARTIFACT_NAME} ${DESTINATION}
  rm -rf distribution/target/distribution

  echo "Created ${DESTINATION}/${ARTIFACT_NAME}"
}

parseArgs $@
package