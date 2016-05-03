#!/usr/bin/env bash
#
# repackage.sh takes a comaas .tar.gz for a tenant and repackages it multiple times
# as to end up with a package for each of the tenant's environments

set -o nounset
set -o errexit

function repackage() {
  mkdir tmp
  tar xfz ${ARTIFACT} -C tmp/

  for prop in distribution/conf/${TENANT}/*; do
    if [[ -f "$prop" || "$prop" == *comaasqa || "$prop" == *local ]]; then
      continue
    fi

    rm -f tmp/conf/*
    cp "$prop"/* tmp/conf/
    cd tmp && tar cfz ../comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz . && cd ..
    echo "Created comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz"
  done

  rm -rf tmp
}

TENANT=$1
GIT_HASH=$2
ARTIFACT=$3
repackage