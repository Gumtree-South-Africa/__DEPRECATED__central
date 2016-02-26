#!/bin/bash

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)

function log() {
	echo "[$(date)]: $*"
}

function fatal() {
	log $*
	exit 1
}

function parseCmd() {
	RUN_TESTS=0
	TENANT=

	while getopts ":tT:" OPTION; do
		case ${OPTION} in
			t) log "Building with tests"; RUN_TESTS=1
				;;
			T) log "Building for tenant $OPTARG"; TENANT="$OPTARG"
				;;
			\?) fatal "Invalid option: -${OPTARG}"
				;;
		esac
	done
}

function main() {
	local start=$(date +"%s")

  # we would use -T1C (one thread per core), but this breaks tests that start an embedded Cassandra instance
  # so for now we run with 1 thread.
	MVN_ARGS="-s etc/settings.xml -T1 clean package"

	if ! [[ ${RUN_TESTS} -eq 1 ]]; then
		log "Skipping the tests"

		MVN_ARGS="$MVN_ARGS -DskipTests=true"
	fi

	if ! [[ -z "$TENANT" ]]; then
		MVN_ARGS="$MVN_ARGS -P${TENANT}"
	else
		log "Building all tenant modules (skipping distribution)"

		# Extract all tenant profile IDs from the POM, as build profile selection is limited (MNG-3328 etc.)

		TENANT=`
		  sed -n '/<profile>/{n;s/.*<id>\(.*\)<\/id>/\1/;p;}' $DIR/../pom.xml | \
		  grep -v 'default\|distribution' | \
		  tr $'\n' ','`

		MVN_ARGS="$MVN_ARGS -P${TENANT}!distribution"
	fi

	mvn $MVN_ARGS

	local end=$(date +"%s")
	local diff=$(($end-$start))
	local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
	log ${time}
}

parseCmd ${ARGS}
main
