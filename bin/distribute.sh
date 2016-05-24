#!/bin/bash

TENANT=$1
GIT_HASH=$2
ARTIFACT=$3

if [ -z "$TENANT" ] || [ -z "$GIT_HASH" ] || [ -z "$ARTIFACT" ] ; then
  echo "$0: <tenant> <git-hash> <artifact>"

  exit 1
fi

# Repackage into packages for each TENANT environment
`dirname $0`/repackage.sh $TENANT $GIT_HASH $ARTIFACT

# Upload or deploy
for PKG in $(ls $BUILD_DIR/comaas-$TENANT*); do 
  DESTINATION=$(echo "$PKG" | sed "s/^.*\-${TENANT}\-\([a-zA-Z0-9]*\)\-.*$/\1/")

  if [[ "$DESTINATION" == "comaasqa" || "$DESTINATION" == "local" ]] ; then
    continue
  fi

  # Deploy for mp demo, upload for all other environments/tenants
  if [[ "$TENANT" == "mp" && "$DESTINATION" == "demo" ]] ; then
    tar -xzf ${PKG} --to-command="cat" ./conf/deploy.conf > deploy.conf
    # This requires the deploy.py script to be on the PATH
    deploy.py --redeploy --config deploy.conf --component ${PKG}
  else
    `dirname $0`/upload.sh $TENANT $DESTINATION $GIT_HASH $PKG
  fi
done
