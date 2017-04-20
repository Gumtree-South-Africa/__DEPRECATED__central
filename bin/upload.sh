#!/usr/bin/env bash
#
# upload.sh takes a package and uploads it to the tenant's deploy server (e.g. https://autodeploy.mobile.corp.de)

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: upload.sh <tenant> <git_hash> <package> <timestamp> <?destination>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  GIT_HASH=$2
  PACKAGE=$3
  TIMESTAMP=$4
}

# map to lookup the upload hosts for a tenant
declare -A HOSTS=(
  ["ebayk"]="https://comaas-uploader:ohy9Te#hah9U@kautodeploy.corp.mobile.de/storage/belen-productive-deployment-releases/com/ecg/ebayk/comaas/"
  ["kjca"]='http://comaas:bMv!Yne7Apj3F4pW@nexus.kjdev.ca/content/repositories/comaas/ecg/comaas/versions/'
  ["gtau"]=""
)

function upload() {
    # prefer a specific environment; otherwise default to the tenant default
    HOST_VALUE="${HOSTS[$TENANT]}"

    if [ -z "$HOST_VALUE" ]; then
        # Don't do anything if we don't know where to upload to.
        return
    fi

    readonly start=$(date +"%s")

    readonly HOST=${HOST_VALUE}${TIMESTAMP}-${GIT_HASH}/
    readonly URL=$(echo ${HOST} | cut -d'@' -f2)

    echo "Uploading $PACKAGE to $URL"

    set -o xtrace
    curl -k -s -X PUT --upload-file "${PACKAGE}" "${HOST}"
    set +o xtrace

    readonly end=$(date +"%s")
    readonly diff=$(($end-$start))
    readonly time=$(printf "%d min %d sec" $(($diff / 60)) $(($diff % 60)))

    echo "Finished uploading/syncing $PACKAGE in $time."
}

parseArgs $@

if [ ${TENANT} == "gtuk" ]; then
    echo "Uploading the GTUK debian package to repositories.ecg.so"
    DEB_PACK="builds/*${GIT_HASH}*.deb"
    if [ ! -f ${DEB_PACK} ]; then
        echo "Package to upload not found: $DEB_PACK"
        exit 1
    fi
    ./bin/upload_ecg_repos.sh ${TENANT} ${DEB_PACK} ${TIMESTAMP} legacy
    exit
fi
if [ ${TENANT} == "mp" ]; then
    echo "Uploading the legacy MP package to the iCAS deployer"
	readonly PREFIX="comaas"
	readonly REMOTE_DIR=/opt/tarballs/ecg-comaas
	readonly RELEASE_FOLDER="${PREFIX}-${TIMESTAMP}"
	readonly KEEPVERSIONS=20
	readonly HOST_VALUE="builder@deploy001.esh.ops.prod.icas.ecg.so"


	echo "Syncing $PACKAGE to ${HOST_VALUE} ..."

	set -o xtrace
	ssh ${HOST_VALUE} "mkdir -p ${REMOTE_DIR}/${RELEASE_FOLDER}"
	rsync -avv --no-times --no-o --no-p --size-only \
		${PACKAGE} \
		${HOST_VALUE}:${REMOTE_DIR}/${RELEASE_FOLDER}/$(basename $PACKAGE)

#	echo "Cleaning up old versions on ${HOST_VALUE} ..."
#	ssh ${HOST_VALUE} "ls -td ${REMOTE_DIR}/${PREFIX}-* | sed -ne'1,${KEEPVERSIONS}!p' | xargs -r rm -rv"
	set +o xtrace
    exit
fi

if [[ "$TENANT" == mde ]] ; then
    echo "Uploading not supported for $TENANT, because it's already live in the cloud"
    exit
fi

upload
