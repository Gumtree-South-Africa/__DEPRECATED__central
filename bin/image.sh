#!/bin/sh
#
# Create a Docker image for the provided tenant, with the current git version

function usage() {
  echo "Usage: $0 <tenant> <env> <artifact>"
  echo "   tenant:    the long tenant name, e.g. gtuk"
  echo "   env:       the environment to build for, like sandbox or prod"
  echo "   artifact:  the packaged .tar.gz to be included in the image"
  exit 1
}
[[ $# != 3 ]] && usage

readonly tenant=${1}
readonly env=${2}
readonly source_file=${3}
readonly source_dir=distribution/target/docker/
readonly git_hash=$(git rev-parse --short HEAD)

printf "Building Docker image\ntenant: ${tenant}\nenvironment: ${env}\nartifact: ${source_file}\n\n"
mkdir -p ${source_dir}
rm -rf ${source_dir}/distribution*

tar xf ${source_file} -C ${source_dir}

readonly image_name="docker-registry.ecg.so/comaas/comaas-${tenant}:${git_hash}"

docker build \
    --tag ${image_name} \
    --label tenant=${tenant} \
    --label env=${env} \
    --label githash=${git_hash} \
    .

docker push ${image_name}

rm -rf ${source_dir}

echo "Created Docker image ${image_name} with the following labels:"
docker inspect ${image_name} | jq '.[].Config.Labels'
echo
