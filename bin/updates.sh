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
	for i in $DIR/core-{api,runtime,graphite}/README.md $DIR/{filters,postprocessors,messagecenters,integration-tests}/*/README.md ; do
		IDIR=$(basename `dirname $i`)

		MATCH=( `sed -n -e 's/.*\(https:\/\/github.*$\)/\1/p;s/.* hash: \(.*\))$/\1/p' $i` )
		GITURL=( `echo ${MATCH[0]} | sed 's/https:\/\//git@/g' | sed 's/github.corp.ebay.com\//github.corp.ebay.com:/g'` )
		HEAD=`git ls-remote ${GITURL} HEAD | cut -d$'\t' -f1`

		if ! [ -z "${MATCH[1]:-}" ] && ! [ -z "$HEAD" ] ; then
			printf "Checking %-30s  ..\t[ %s ]\n" "$IDIR" \
			  $([[ "$HEAD" == "${MATCH[1]}" ]] && echo "OK" || echo "OUTDATED")
		fi
	done
}

main
