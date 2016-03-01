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
		HEAD=`git ls-remote ${MATCH[0]} HEAD | cut -d$'\t' -f1`

		if ! [ -z "${MATCH[1]:-}" ] && ! [ -z "$HEAD" ] ; then
			printf "Checking %-30s  ..\t[ %s ]\n" "$IDIR" \
			  $([[ "$HEAD" == "${MATCH[1]}" ]] && echo "OK" || echo "OUTDATED")
		fi
	done
}

main
