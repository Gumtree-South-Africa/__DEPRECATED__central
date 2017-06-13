#!/usr/bin/env bash

# Provision swift infra (containers) for attachment storage in prod

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
  		readonly KEYSTONE="https://keystone.ams1.cloud.ecg.so/v2.0"
  		readonly PROJECTID="d0b4d1b698f940e784e33f0ed276bb7a"
  elif [[ "${DC,,}" == "dus1" ]]; then
  		readonly KEYSTONE="https://keystone.dus1.cloud.ecg.so/v2.0"
  		readonly PROJECTID="10a36ca6d44c46a1bd8b2ae1f92204b9"
  else
		echo "DC value is either ams1 or dus1"
		exit
  fi

}


function createInfra() {

for i in `seq 0 $SWIFT_CONTAINER_NUM`; do
    CONTAINER_NAME="$SWIFT_CONTAINER_NAME-$i"
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-prod --os-region-name "$DC" --os-tenant-name comaas-prod --os-username $USER --os-password "$password" post $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-prod --os-region-name "$DC" --os-tenant-name comaas-prod --os-username $USER --os-password "$password" post -w $PROJECTID:comaas-prod-swift $CONTAINER_NAME
	swift --os-auth-url "$KEYSTONE" --os-project-name comaas-prod --os-region-name "$DC" --os-tenant-name comaas-prod --os-username $USER --os-password "$password" post -r ".r:*,.rlistings,$PROJECTID:comaas-prod-swift,$PROJECTID:comaas-prod-swift-readonly" $CONTAINER_NAME
done

}

parseArgs $@
createInfra

# Test - should be

#swift --os-auth-url https://keystone.ams1.cloud.ecg.so/v2.0 --os-tenant-name comaas-prod --os-username ptroshin --os-password [] stat ebayk-test-container-99
#         Account: d0b4d1b698f940e784e33f0ed276bb7a
#       Container: ebayk-test-container-99
#         Objects: 0
#           Bytes: 0
#        Read ACL: .r:*,.rlistings,d0b4d1b698f940e784e33f0ed276bb7a:comaas-prod-swift,d0b4d1b698f940e784e33f0ed276bb7a:comaas-prod-swift-readonly
#       Write ACL: d0b4d1b698f940e784e33f0ed276bb7a:comaas-prod-swift
#         Sync To:
#        Sync Key:
#   Accept-Ranges: bytes
#      X-Trans-Id: tx68a41784dac04578932d3-0058e650cd
#X-Storage-Policy: Policy-0
#   Last-Modified: Thu, 06 Apr 2017 14:25:18 GMT
#     X-Timestamp: 1491480775.10329
#    Content-Type: text/plain; charset=utf-8