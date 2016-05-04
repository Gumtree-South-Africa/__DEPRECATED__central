#!/usr/bin/env bash
#
# upload.sh takes a package and uploads it to the tenant's deploy server (e.g. https://autodeploy.mobile.corp.de)

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: upload.sh <tenant> <git_hash> <package>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  GIT_HASH=$2
  PACKAGE=$3
}

# map to lookup the upload hosts for a tenant
declare -A HOSTS=(
  ["ebayk"]="https://comaas-uploader:ohy9Te#hah9U@kautodeploy.corp.mobile.de/storage/belen-productive-deployment-releases/ecg/comaas/versions/" \
  ["mde"]="https://comaas-uploader:ohy9Te#hah9U@autodeploy.corp.mobile.de/storage/hosted-mobile-deployment-team-releases/ecg/ecg-comaas/versions/"
  #mde prod: https://autodeploy.corp.mobile.de/storage/hosted-mobile-deployment-productive-releases/ecg/comaas/versions/
)

function upload() {
  # host to upload to
  readonly HOST=${HOSTS[$TENANT]}${GIT_HASH}/
  readonly URL=$(echo ${HOST} | cut -d'@' -f2)

  echo "Uploading $PACKAGE to $URL"
  readonly start=$(date +"%s")

  set -o xtrace
  curl -k -vvv --progress-bar -X PUT --upload-file "${PACKAGE}" "${HOST}" > /dev/null
  set +o xtrace

  readonly end=$(date +"%s")
  readonly diff=$(($end-$start))
  readonly time=$(printf "%d min %d sec" $(($diff / 60)) $(($diff % 60)))

  echo "Finished uploading $PACKAGE to $URL in $time."
}

parseArgs $@
upload
