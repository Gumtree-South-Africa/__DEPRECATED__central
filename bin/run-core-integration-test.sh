#!/bin/bash
# Run ./run-core-integration-test.sh -Dtest=<testname> to run a specific core-integration test

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly CASSANDRA_DIR="$DIR/../cassandra_tmp"
readonly CASSANDRA_PID="cassandra.pid"
readonly REVISION="$(git rev-parse --short HEAD)"

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

function startCassandra() {
    log "Starting cassandra"
    rm -rf ${CASSANDRA_DIR}
    mkdir ${CASSANDRA_DIR}
    /opt/cassandra/bin/cassandra -p ${CASSANDRA_PID} "-Dcassandra.storagedir=$CASSANDRA_DIR"

    # stop & clean cassandra dir on exit
    trap "stopCassandra" EXIT
}

function stopCassandra() {
    if [[ -e ${CASSANDRA_PID} ]]; then
        log "Stopping cassandra"
        kill $(cat ${CASSANDRA_PID})
        rm ${CASSANDRA_PID}
        rm -rf ${CASSANDRA_DIR}
    fi
}

function main() {
    startCassandra

    local start=$(date +"%s")

    # mvn clean test -DfailIfNoTests=false -Pmp -pl messagecenters/mp-messagecenter -am -Drevision=revision $@
    mvn clean test -DfailIfNoTests=false -pl integration-tests/core-integration-test -am -Drevision=revision $@

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}

    stopCassandra
}

main $@
