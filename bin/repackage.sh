#!/usr/bin/env bash
#
# repackage.sh takes a comaas .tar.gz for a tenant and repackages it multiple times
# as to end up with a package for each of the tenant's environments

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: repackage.sh <tenant> <git_hash> <artifact> <timestamp>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  GIT_HASH=$2
  ARTIFACT=$3
  TIMESTAMP=$4
  cur=$(pwd -P) && cd $(dirname ${ARTIFACT}) && BUILDDIR=$(pwd -P) && cd ${cur}
}

function repackage() {
  mkdir -p tmp
  mkdir -p tmp2
  tar xfz ${ARTIFACT} -C tmp/

  for prop in distribution/conf/${TENANT}/*; do
    if [[ -f "$prop" || "$prop" == *comaasqa || "$prop" == *local ]]; then
      continue
    fi


    PACKAGE_BASE=${BUILDDIR}/comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}-${TIMESTAMP}
    PACKAGE_NAME=comaas-${TENANT}-$(basename "$prop")-${GIT_HASH}-${TIMESTAMP}
    if [[ "$prop" == *noenv ]]; then
      PACKAGE_BASE=${BUILDDIR}/comaas-${TENANT}-${GIT_HASH}-${TIMESTAMP}
      PACKAGE_NAME=comaas-${TENANT}-${GIT_HASH}-${TIMESTAMP}
    fi

    case "$TENANT" in
      ebayk)
        if [ ! -d tmp/comaas ] ; then
          mkdir comaas-tmp && mv tmp/* comaas-tmp/ && mv comaas-tmp tmp/comaas
        fi
        rm -f tmp/comaas/conf/* && cp "$prop"/* tmp/comaas/conf/
        cd tmp && tar cfz ${PACKAGE_BASE}.tar.gz . && cd ..
        echo "Created ${PACKAGE_BASE}.tar.gz"
        ;;
      kjca)
        # Create a tar archive with all the libs
        rm -rf tmp/{bin,conf,log}
        cd tmp && tar cf ${PACKAGE_BASE}.tar . && cd ..
        echo "Created ${PACKAGE_BASE}.tar"
        # Create a single JAR with only a META-INF/MANIFEST.MF
        cd tmp/lib && mkdir ../META-INF
        echo -e "Manifest-Version: 1.0\nArchiver-Version: Bash Archiver\nBuilt-By: thecomaasteam" >> ../META-INF/MANIFEST.MF
        echo "Clss-Path: $(ls -1 * | xargs)" | fold -w 69 | sed -e 's/^/ /' -e 's/^ Clss-Path/Class-Path/' >> ../META-INF/MANIFEST.MF
        echo -e "Created-By: COMaaS Team 1.0.0.AWESOME\nBuild-Jdk: $(javac -version 2>&1 | sed -e 's/javac//' -e 's/\ //g')" >> ../META-INF/MANIFEST.MF
        echo -e "Main-Class: com.ecg.replyts.core.runtime.ReplyTS" >> ../META-INF/MANIFEST.MF
        cd .. && rm -rf lib && zip -r ${PACKAGE_BASE}.jar . && cd ..
        echo "Created ${PACKAGE_BASE}.jar"
        ;;
      mp)
      # Repackaging for MP
        DISTRIB_ARTIFACT=nl.marktplaats.mp-replyts2_comaas-$(basename "$prop")-${GIT_HASH_FULL}-${TIMESTAMP}
        rm -rf tmp2
        mkdir -p tmp2/${DISTRIB_ARTIFACT}
        cp -r tmp/* tmp2/${DISTRIB_ARTIFACT}/
        rm -f tmp2/${DISTRIB_ARTIFACT}/conf/* && cp "$prop"/* tmp2/${DISTRIB_ARTIFACT}/conf/
        cd tmp2
        mv ${DISTRIB_ARTIFACT}/bin/comaas ${DISTRIB_ARTIFACT}/bin/mp-replyts2
        tar cfz ${BUILDDIR}/${DISTRIB_ARTIFACT}.tar.gz . && cd ..
        echo "Created ${BUILDDIR}/${DISTRIB_ARTIFACT}.tar.gz"
        continue
      ;;
      mde)
      # Repackaging for MDE
        rm -rf tmp2
        mkdir -p tmp2/comaas-mde
        cp -r tmp/* tmp2/comaas-mde/
        rm -f tmp2/comaas-mde/conf/* && cp "$prop"/* tmp2/comaas-mde/conf/
        cd tmp2
        tar cfz ${PACKAGE_BASE}.tar.gz . && cd ..
        HOMEDIR=$PWD
        cd ${BUILDDIR}
        md5sum ${PACKAGE_NAME}.tar.gz > ${PACKAGE_NAME}.tar.gz.md5
        cd $HOMEDIR
        echo "Created package for mde ${PACKAGE_BASE}.tar.gz"
        continue
      ;;
      *)
        rm -f tmp/conf/* && cp "$prop"/* tmp/conf/
        cd tmp && tar cfz ${PACKAGE_BASE}.tar.gz . && cd ..
        echo "Created ${PACKAGE_BASE}.tar.gz"
        ;;
    esac

  done

  rm -rf tmp tmp2
}

parseArgs $@
GIT_HASH_FULL=$(git rev-parse HEAD)
repackage
