#!/usr/bin/env bash
#
# upload.sh takes a package and uploads it to the tenant's deploy server (e.g. https://autodeploy.mobile.corp.de)

set -o nounset
set -o errexit

# map to lookup the upload hosts for a tenant
declare -A HOSTS=(
  ["ebayk"]="https://comaas-uploader:ohy9Te#hah9U@kautodeploy.corp.mobile.de/storage/belen-productive-deployment-releases/ecg/comaas/" \
  ["mde"]="https://comaas-uploader:ohy9Te#hah9U@autodeploy.corp.mobile.de/storage/belen-productive-deployment-releases/ecg/comaas/"
)

# package of the form distribution-456abf9-ebayk-prod.tar.gz
readonly PACKAGE=$1
readonly TENANT=$(echo ${PACKAGE} | cut -d'-' -f3)

# host to upload to
readonly HOST=${HOSTS[$TENANT]}
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
