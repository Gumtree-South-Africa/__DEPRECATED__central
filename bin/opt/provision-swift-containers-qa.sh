#!/usr/bin/env bash

# Provision swift infra (containers) for attachment storage in comaas-qa

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
  echo -n "Your LDAP Password:"
  read -s password
  echo

  if [[ "${DC,,}" == "ams1" ]]; then
  		KEYSTONE="https://keystone.ams1.cloud.ecg.so/v2.0"
  elif [[ "${DC,,}" == "dus1" ]]; then
  		KEYSTONE="https://keystone.dus1.cloud.ecg.so/v2.0"
  else
		echo "DC value is either ams1 or dus1"
		exit
  fi

}


function createInfra() {

for i in `seq 0 $SWIFT_CONTAINER_NUM`; do
    CONTAINER_NAME="$SWIFT_CONTAINER_NAME-$i"
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-qa --os-region-name "$DC" --os-tenant-name comaas-qa --os-username $USER --os-password "$password" post $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-qa --os-region-name "$DC" --os-tenant-name comaas-qa --os-username $USER --os-password "$password" post -w '82e1bce9d62c4189968bd19b395ef8ef:comaas-qa-swift' $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-qa --os-region-name "$DC" --os-tenant-name comaas-qa --os-username $USER --os-password "$password" post -r '.r:*,.rlistings,82e1bce9d62c4189968bd19b395ef8ef:comaas-qa-swift' $CONTAINER_NAME
done

}

parseArgs $@
createInfra

# Test - should be
#swift --os-auth-url https://keystone.ams1.cloud.ecg.so/v2.0 --os-tenant-name comaas-qa --os-username ptroshin --os-password [] stat test-container-9
