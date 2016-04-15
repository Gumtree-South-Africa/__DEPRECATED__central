#!/bin/bash

set -o nounset
set -o errexit

readonly DIR=$(dirname $0)/..

function log() {
	echo "[$(date)]: $*"
}

function fatal() {
	log $*
	exit 1
}

function main() {
	for i in $DIR/core-{api,runtime,graphite}/README.md $DIR/{eventpublishers,filters,integration-tests,listeners,messagecenters,postprocessors,resultinspectors}/*/README.md ; do
		IDIR=$(basename `dirname $i`)

		# find github url + git hash in Readme
		MATCH=( `sed -n -e 's/.*\(https:\/\/github.*$\)/\1/p;s/.* hash: \(.*\))$/\1/p' $i` )
		GITURL=${MATCH[0]:-}
		GITHASH=${MATCH[1]:-}
		if [ -z "${GITURL}" ]; then
		  echo "Could not find git url in ${IDIR}"
		  exit 1
		elif [ -z "${GITHASH}" ]; then
		  echo "Could not find git hash in ${IDIR}"
		  exit 1
		fi

		# rewrite https url into ssh url
		GITSSH=$(echo ${GITURL} | sed 's/https:\/\//git@/g' | sed 's/github.corp.ebay.com\//github.corp.ebay.com:/g')

		# find the HEAD git hash
		HEAD=$(git ls-remote ${GITSSH} HEAD | cut -f1)
		if [ -z "{HEAD}" ]; then
		  echo "Could not get HEAD from ${GITSSH}"
		  exit 1
		fi

		printf "Checking %-30s  ..\t\t[ %s ]\n" "$IDIR" \
			  $([[ "$HEAD" == "${GITHASH}" ]] && echo "OK" || echo "OUTDATED")

	done
}

main
