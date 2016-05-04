#!/usr/bin/env bash
#
# jenkins-deploy-comaasqa.sh deploys a comaas package to the comaasqa env

set -o nounset
set -o errexit

function usage() {
  cat <<- EOF
  Usage: jenkins-deploy-comaasqa.sh <tenant> <artifact>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  ARTIFACT=$2
}

function deploy() {
	MD5=($(md5sum -b ${ARTIFACT}))

	cp distribution/nomad/comaas_deploy_jenkins.json comaas_deploy_jenkins.json
	sed -i "s/PARENT_NAME/repo-server/" comaas_deploy_jenkins.json
	sed -i "s/TENANT/$TENANT/g" comaas_deploy_jenkins.json
	sed -i "s/md5/md5:$MD5/" comaas_deploy_jenkins.json
	# use ~ separator here since $ARTIFACT might contain slashes
	sed -i "s~ARTIFACT~$ARTIFACT~" comaas_deploy_jenkins.json

	curl -X POST -d @comaas_deploy_jenkins.json http://consul001:4646/v1/jobs --header "Content-Type:application/json"

	# TODO(gg): health/restart check
	echo "Deployed $ARTIFACT to comaasqa"
}

parseArgs $@
deploy