#!/usr/bin/env bash
# Provision swift infra (containers) for attachment storage in comaas-lp

# Follow instructions here to install swift client
#https://ecgwiki.corp.ebay.com/display/OrgTechnologyEngSupport/eCG+Cloud+overview
set -x

function usage() {
  cat <<- EOF
  Usage: provision-swift-containers.sh <datacenter> <swift_container_name> <swift_container_num>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  readonly DC=$1
  readonly SWIFT_CONTAINER_NAME=$2
  readonly SWIFT_CONTAINER_NUM=$3
  echo -n "Swift Tenant Username:"
  read  USER
  echo -n "Swift Tenant Password:"
  read -s password
  echo

  if [[ "${DC}" == "ams1" ]]; then
  		KEYSTONE="https://keystone.ams1.cloud.ecg.so/v2.0"
  elif [[ "${DC}" == "dus1" ]]; then
  		KEYSTONE="https://keystone.dus1.cloud.ecg.so/v2.0"
  else
		echo "DC value is either ams1 or dus1"
		exit
  fi

}


function createInfra() {

for i in `seq 0 $SWIFT_CONTAINER_NUM`; do
    CONTAINER_NAME="$SWIFT_CONTAINER_NAME-$i"
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-lp --os-region-name "$DC" --os-tenant-name comaas-lp --os-username $USER --os-password "$password" post $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-lp --os-region-name "$DC" --os-tenant-name comaas-lp --os-username $USER --os-password "$password" post -w '7560cd31be704bc7a7bee319352b5f1b:comaas-qa-swift' $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-lp --os-region-name "$DC" --os-tenant-name comaas-lp --os-username $USER --os-password "$password" post -r '.r:*,.rlistings,7560cd31be704bc7a7bee319352b5f1b:comaas-qa-swift' $CONTAINER_NAME
done

}

parseArgs $@
createInfra

# Test - should be
#swift --os-auth-url https://keystone.ams1.cloud.ecg.so/v2.0 --os-tenant-name comaas-qa --os-username ptroshin --os-password [] stat test-container-9
