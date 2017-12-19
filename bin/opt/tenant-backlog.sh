#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "${0##*/} [au ek mo mp uk] [prod sandbox playground]"
    echo
    echo "Show the commits that are not released to this tenant and environment"
    exit
fi

TENANT=$1
ENVIRONMENT=$2

REVISION=`curl -s http://$TENANT.$ENVIRONMENT.comaas.cloud/health | jq -r .version`

git -C ${0%/*} log --oneline $REVISION..origin/master
