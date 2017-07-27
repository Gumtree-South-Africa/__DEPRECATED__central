#!/usr/bin/env bash

# repackage.sh takes a comaas .tar.gz for a tenant and repackages for the tenant's legacy environment

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

function portable-md5() {
  local PACKAGE=$1
  if builtin command -v md5 > /dev/null; then
    # mac osx
    md5 -r ${PACKAGE}
  else
    # linux
    md5sum ${PACKAGE}
  fi
}

function repackage() {
    mkdir -p tmp
    mkdir -p tmp2
    tar xfz ${ARTIFACT} -C tmp/

    PACKAGE_NAME=comaas-${TENANT}-legacy-${TIMESTAMP}-${GIT_HASH}
    PACKAGE_BASE=${BUILDDIR}/${PACKAGE_NAME}

    echo Repackaging for \"${TENANT}\"

    case "$TENANT" in
      gtuk)
        # GTUK wants a debian package with systemd startup files.

        echo "Actually, let's not. When they need a build, I'll create one by hand. RP"
        exit

        mkdir -p tmp3/usr/lib/replyts2
        cp -r tmp/bin tmp/lib tmp/conf tmp3/usr/lib/replyts2

        mkdir -p tmp3/usr/lib/systemd/system
        cp distribution/gumtree-replyts2-deb-package/src/deb/systemd/system/replyts2.service tmp3/usr/lib/systemd/system

        cp distribution/gumtree-replyts2-deb-package/src/deb/replyts2-control/p* tmp3/

        tar xf distribution/target/distribution-gtuk-comaasqa.tar.gz distribution/lib/core-runtime-${GIT_HASH}.jar
        GTUK_VERSION=$(unzip -p distribution/lib/core-runtime-${GIT_HASH}.jar META-INF/MANIFEST.MF | grep Build-Date | awk '{print $2}' | tr -d '\r')
        rm -rf distribution/lib/

        mkdir -p tmp3/DEBIAN
        cp distribution/gumtree-replyts2-deb-package/src/deb/replyts2-control/control tmp3/DEBIAN
        sed -i'.bak' s/%VERSION%/5.0.${GTUK_VERSION}/ tmp3/DEBIAN/control

        set +o errexit
        path_to_dpkg_deb=$(which dpkg-deb)
        set -o errexit
        if [ -x "$path_to_dpkg_deb" ]; then
          rm -f gumtree.uk.deb
          "$path_to_dpkg_deb" --build tmp3 gumtree.uk.deb
        else
          docker run -v $PWD:/build debian dpkg-deb --build /build/tmp3 /build/gumtree.uk.deb
        fi

        # This package name is a GTUK requirement.
        mv -vf gumtree.uk.deb "$BUILDDIR/gumtree-replyts2_5.0.${GTUK_VERSION}-${GIT_HASH}_all.deb"

        rm -rf tmp3
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
      it)
        echo "No need to repackage IT"
        ;;
      *)
        echo "Unknown tenant $TENANT"
        exit 1
        ;;
    esac

    rm -rf tmp tmp2
}

parseArgs $@

if [[ "$TENANT" == "mde" ]] || [[ "$TENANT" == "mp" ]] || [[ "$TENANT" == "ebayk" ]] || [[ "$TENANT" == "gtau" ]]; then
    echo "Repackaging not supported for $TENANT, because it's already live in the cloud"
    exit
fi

GIT_HASH_FULL=$(git rev-parse HEAD)
repackage
