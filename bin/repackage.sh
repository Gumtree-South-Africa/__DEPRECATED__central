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

    case "$TENANT" in
      ebayk)
        if [ ! -d tmp/comaas ] ; then
          mkdir comaas-tmp && mv tmp/* comaas-tmp/ && mv comaas-tmp tmp/comaas
        fi
        rm -f tmp/comaas/conf/* && cp "$prop"/* tmp/comaas/conf/
        cd tmp && tar cfz ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz . && cd ..
        ;;
      kjca)
        # Create a tar archive with all the libs
        rm -rf tmp/{bin,conf,log}
        cd tmp && tar cf ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar . && cd ..
        echo "Created ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar"
        # Create a single JAR with only a META-INF/MANIFEST.MF
        cd tmp/lib && mkdir ../META-INF
        echo -e "Manifest-Version: 1.0\nArchiver-Version: Bash Archiver\nBuilt-By: thecomaasteam" >> ../META-INF/MANIFEST.MF
        echo "Clss-Path: $(ls -1 * | xargs)" | fold -w 69 | sed -e 's/^/ /' -e 's/^ Clss-Path/Class-Path/' >> ../META-INF/MANIFEST.MF
        echo -e "Created-By: COMaaS Team 1.0.0.AWESOME\nBuild-Jdk: $(javac -version 2>&1 | sed -e 's/javac//' -e 's/\ //g')" >> ../META-INF/MANIFEST.MF
        echo -e "Main-Class: com.ecg.replyts.core.runtime.ReplyTS" >> ../META-INF/MANIFEST.MF
        cd .. && rm -rf lib && zip -r ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.jar . && cd ..
        ;;
      *)
        rm -f tmp/conf/* && cp "$prop"/* tmp/conf/
        cd tmp && tar cfz ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz . && cd ..
        ;;
    esac

    echo "Created ${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}.tar.gz"
  done

  rm -rf tmp
}

parseArgs $@
repackage
