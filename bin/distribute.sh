#!/bin/bash

TENANT=$1
GIT_HASH=$2
ARTIFACT=$3
BUILD_DIR=$4

if [ -z "$TENANT" ] || [ -z "$GIT_HASH" ] || [ -z "$ARTIFACT" ] || [ -z "$BUILD_DIR" ]; then
  echo "$0: <tenant> <git-hash> <artifact> <build_dir>"
  exit 1
fi

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh $TENANT $GIT_HASH $ARTIFACT

PACKAGE_REGEX=".*/comaas-$TENANT-([0-9a-zA-Z]*)-[0-9a-z]\..*"

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

  # Deploy for mp demo, upload for all other environments/tenants
  if [[ "$TENANT" == "mp" && "$DESTINATION" == "demo" ]] ; then
    tar -xzf ${PKG} --to-command="cat" ./conf/deploy.conf > deploy.conf
    # This requires the deploy.py script to be on the PATH
    deploy.py --redeploy --config deploy.conf --logdir . --component ${PKG} --dry-run
  else
    `dirname $0`/upload.sh $TENANT $GIT_HASH $PKG $DESTINATION
  fi
done
