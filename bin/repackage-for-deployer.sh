#!/usr/bin/env bash
#
# repackage-for-deployer.sh takes a comaas .tar.gz and repackages to work with comaas deployer script
# This script should be run from ecg-comaas-central repository root directory e.g. bin/repackage-for-deployer.sh

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: repackage-for-deployer.sh <artifact>
  where <artifact> must end with tar.gz
EOF
  exit
}

function parseArgs() {
  FILEEXT=".tar.gz"
  # check amount of args & whether the filename ends with FILEEXT
  [[ $# == 0 ]] || [[ ${1: -${#FILEEXT} } != $FILEEXT ]] && usage

  filename=$(basename $1 .tar.gz)

  #remove path & extension from file name
  filename="${filename%.*}"

  ARTIFACT=$filename
  parts=(${ARTIFACT//-/ })
  TENANT=${parts[1]}
  ENV=${parts[2]}
  WORKSPACE=${WORKSPACE-$PWD}
}


function repackage() {

  TIMESTAMP=$(date +%C%y%m%d-%H%M)
  GIT_HASH=$(git rev-parse --short HEAD)

  REPACKAGED_ARTIFACT_NAME="comaas-${TENANT}_fordeployer-${ENV}-${TIMESTAMP}-${GIT_HASH}"
  ARTIFACT_NAME="distribution-${TENANT}-${ENV}"
  cd distribution/target
  mkdir -p repackage
  tar xvfz $ARTIFACT$FILEEXT
  cd distribution
  rm -rf conf
  mkdir -p conf
  cp ../../conf/$TENANT/$ENV/* conf
  cd ..
  # now in distribution/target/
  mkdir -p repackage/${REPACKAGED_ARTIFACT_NAME}/
  mv distribution/* repackage/${REPACKAGED_ARTIFACT_NAME}/
  tar czf $WORKSPACE/${REPACKAGED_ARTIFACT_NAME}.tar.gz -C repackage .

  echo "Created ${WORKSPACE}/${REPACKAGED_ARTIFACT_NAME}.tar.gz"
}

parseArgs $@
repackage
