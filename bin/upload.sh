#!/usr/bin/env bash
#
# upload.sh takes a package and uploads it to the tenant's deploy server (e.g. https://autodeploy.mobile.corp.de)

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: upload.sh <tenant> <destination> <git_hash> <package>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  DESTINATION=$2
  GIT_HASH=$3
  PACKAGE=$4
}

# map to lookup the upload hosts for a tenant
declare -A HOSTS=(
  ["mp"]="mp-deploy001.opslp.ams01.marktplaats.nl"
  ["ebayk"]="https://comaas-uploader:ohy9Te#hah9U@kautodeploy.corp.mobile.de/storage/belen-productive-deployment-releases/ecg/comaas/versions/" \
  ["mde"]="https://comaas-uploader:ohy9Te#hah9U@autodeploy.corp.mobile.de/storage/hosted-mobile-deployment-team-releases/ecg/ecg-comaas/versions/"

  # Overrides for specific environments
  ["mp-prod"]="deploy001.esh.ops.prod.icas.ecg.so"
  ["mde-prod"]="https://autodeploy.corp.mobile.de/storage/hosted-mobile-deployment-productive-releases/ecg/comaas/versions/"
)

# map to lookup upload methods for a tenant
declare -A METHODS=(
  ["mp"]="rsync"
  ["ebayk"]="autodeployer"
  ["mde"]="autodeployer"
)

function upload() {
  # prefer a specific environment; otherwise default to the tenant default
  HOST_VALUE="${HOSTS[${TENANT}-${DESTINATION}]:-}"
  if [ -z "$HOST_VALUE" ] ; then
    HOST_VALUE="${HOSTS[$TENANT]}"
  fi

  METHOD="${METHODS[$TENANT]}"

  readonly start=$(date +"%s")

  case ${METHOD} in
    autodeployer) 
      # host to upload to
      readonly HOST=${HOST_VALUE}${GIT_HASH}/
      readonly URL=$(echo ${HOST} | cut -d'@' -f2)

      echo "Uploading $PACKAGE to $URL"

      set -o xtrace
      curl -k -vvv --progress-bar -X PUT --upload-file "${PACKAGE}" "${HOST}" > /dev/null
      set +o xtrace

      ;;
    rsync)
      readonly PREFIX="mpreplyts2"
      readonly REMOTE_DIR=/opt/tarballs/replyts2
      readonly RELEASE_FOLDER="${PREFIX}-$(date +%Y%m%d%H%M%S)"
      readonly KEEPVERSIONS=20

      echo "Syncing $PACKAGE to ${HOST_VALUE} ..."

      set -o xtrace
      ssh builder@${HOST_VALUE} "mkdir -p ${REMOTE_DIR}/${RELEASE_FOLDER}"
      rsync -avv --no-times --no-o --no-p --size-only \
        ${PACKAGE} \
        builder@${HOST_VALUE}:${REMOTE_DIR}/${RELEASE_FOLDER}/$(basename $PACKAGE)

      echo "Cleaning up old versions on ${HOST_VALUE} ..."
      ssh builder@${HOST_VALUE} "ls -td ${REMOTE_DIR}/${PREFIX}-* | sed -ne'1,${KEEPVERSIONS}!p' | xargs -r rm -rv"
      set +o xtrace

      ;;
    esac

    readonly end=$(date +"%s")
    readonly diff=$(($end-$start))
    readonly time=$(printf "%d min %d sec" $(($diff / 60)) $(($diff % 60)))

    echo "Finished uploading/syncing $PACKAGE in $time."
}

parseArgs $@
upload
