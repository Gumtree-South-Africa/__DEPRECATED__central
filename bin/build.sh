#!/usr/bin/env bash

function log() {
	echo "[$(date)]: $*"
}

function main() {
	local start=$(date +"%s")
	local end=$(date +"%s")

	mvn -s etc/settings.xml clean package

	local diff=$(($end-$start))
	local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
	log $time
}

main