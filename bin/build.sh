#!/bin/bash

set -o nounset
set -o errexit

readonly ARGS="$@"

function log() {
	echo "[$(date)]: $*"
}

function fatal() {
	log $*
	exit 1
}

function parseCmd() {
	RUN_TESTS=0

	while getopts ":t" OPTION; do
		case ${OPTION} in
			t) log "Building with tests"; RUN_TESTS=1
				;;
			\?) fatal "Invalid option: -${OPTARG}"
				;;
		esac
	done
}

function main() {
	local start=$(date +"%s")

	if [[ ${RUN_TESTS} == 1 ]]; then
		mvn -s etc/settings.xml clean package
	else
		mvn -s etc/settings.xml clean package -DskipTests=true
	fi

	local end=$(date +"%s")
	local diff=$(($end-$start))
	local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
	log ${time}
}

parseCmd ${ARGS}
main