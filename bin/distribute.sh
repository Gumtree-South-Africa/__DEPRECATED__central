#!/bin/bash

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: distribute.sh <tenant> <git_hash> <artifact> <timestamp> <build_dir>
EOF
  exit
}

# check amount of args
[[ $# == 0 ]] && usage

TENANT=$1
GIT_HASH=$2
ARTIFACT=$3
TIMESTAMP=$4
BUILD_DIR=$5

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh $TENANT $GIT_HASH $ARTIFACT $TIMESTAMP

if [[ "$TENANT" == "mp" ]]; then
  MP_PACKAGE_REGEX=".*/nl.marktplaats.mp-replyts2_comaas-([0-9a-zA-Z]+)-.*"

  # Upload or deploy (only the 3 most recent packages)
  for PKG in $(ls -tdr ${BUILD_DIR}/nl.marktplaats.mp-replyts2* | tail -n3); do

    if [[ ${PKG} =~ ${MP_PACKAGE_REGEX} ]]; then
      DESTINATION="${BASH_REMATCH[1]}"
    else
      DESTINATION=""
    fi

    if [[ "$DESTINATION" == "comaasqa" || "$DESTINATION" == "local" ]] ; then
      continue
    fi

    # Deploy for mp demo, upload for all other environments/tenants
    if [[ "$TENANT" == "mp" && "$DESTINATION" == "demo" ]] ; then
      # This requires the deploy.py script to be on the PATH
      deploy.py --redeploy --config distribution/conf/mp/demo/deploy.conf --logdir . --component ${PKG}
    else
      `dirname $0`/upload.sh $TENANT $GIT_HASH $PKG $TIMESTAMP $DESTINATION
    fi
  done
else
  PACKAGE_REGEX=".*/comaas-$TENANT-([0-9a-zA-Z]+)-[0-9a-z]+\..*"
  # Upload or deploy
  for PKG in $(ls $BUILD_DIR/comaas-$TENANT*); do

    if [[ ${PKG} =~ ${PACKAGE_REGEX} ]]; then
      DESTINATION="${BASH_REMATCH[1]}"
    else
      DESTINATION=""
    fi

    if [[ "$DESTINATION" == "comaasqa" || "$DESTINATION" == "local" ]] ; then
      continue
    fi

    `dirname $0`/upload.sh $TENANT $GIT_HASH $PKG $TIMESTAMP $DESTINATION
  done
fi
