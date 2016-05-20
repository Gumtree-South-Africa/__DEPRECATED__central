#!/usr/bin/env bash
#
# repackage.sh takes a comaas .tar.gz for a tenant and repackages it multiple times
# as to end up with a package for each of the tenant's environments

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: repackage.sh <tenant> <git_hash> <artifact>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  GIT_HASH=$2
  ARTIFACT=$3
  cur=$(pwd -P) && cd $(dirname ${ARTIFACT}) && BUILDDIR=$(pwd -P) && cd ${cur}
}

function repackage() {
  mkdir tmp
  tar xfz ${ARTIFACT} -C tmp/

  for prop in distribution/conf/${TENANT}/*; do
    if [[ -f "$prop" || "$prop" == *comaasqa || "$prop" == *local ]]; then
      continue
    fi

    rm -f tmp/conf/*
    cp "$prop"/* tmp/conf/
    cd tmp && tar cfz ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz . && cd ..
    echo "Created ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz"
  done

  rm -rf tmp
}

parseArgs $@
repackage
