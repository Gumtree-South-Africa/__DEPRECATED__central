#!/usr/bin/env bash

# Adjustment of run-locally.sh script for new Jenkins (extracting environment start/stop to pipeline)
# This script starts Comaas with the "docker" profile, then checks the health to see if all beans are wired correctly.

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly COMAAS_PID="$PWD/comaas.pid"

readonly ATTEMPTS=60
readonly HEALTH_CHECK_DELAY=3

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

function parseCmd() {
    # check number of args
    [[ $# == 0 ]] && usage
    TENANT=$1
    DOCKER_PROJECT=${2:-comaasdocker}
    DOCKER_NETWORK="${DOCKER_PROJECT}_default"
    DOCKER_CONSUL="${DOCKER_PROJECT}_consul_1"
}

function findOpenPort() {
    for i in $(seq 1 ${ATTEMPTS}); do
        local PORT=$((9000 + RANDOM % 999))

        set +o errexit
        lsof -ni:${PORT} 2>&1 >/dev/null;
        local ec=$?
        set -o errexit

        if [[ ${ec} -gt 0 ]]; then
            break
        fi
    done

    if [[ ${i} -ge ${ATTEMPTS} ]]; then
        fatal "Could not find open port to start Comaas."
    fi

    echo ${PORT}
}

function startComaas() {
    log "Starting Comaas"
    COMAAS_HTTP_PORT=$(findOpenPort)
    log "Starting comaas on port $COMAAS_HTTP_PORT"

    (cd distribution/target
    tar xfz comaas-${TENANT}_1-SNAPSHOT-comaas.tar.gz
    cd comaas-${TENANT}_1-SNAPSHOT
    mkdir -p log

    COMAAS_HTTP_PORT=${COMAAS_HTTP_PORT} \
    java \
    -DlogDir=log \
    -Dtenant=${TENANT} \
    -Dservice.discovery.port=8599 \
    -Dlogging.service.structured.logging=false \
    -XX:-HeapDumpOnOutOfMemoryError \
    -Djava.awt.headless=true \
    -Dcom.datastax.driver.FORCE_NIO=true \
    -XX:+PrintGCDetails \
    -Xloggc:log/gc.log \
    -verbose:gc \
    -XX:+PrintGCTimeStamps \
    -XX:+PrintGCDateStamps \
    -XX:+PrintTenuringDistribution \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=7 \
    -XX:GCLogFileSize=128M \
    -XX:+PrintConcurrentLocks \
    -XX:+PrintClassHistogram \
    -XX:+PrintStringTableStatistics \
    -classpath lib/\* \
    -Dapp.name=comaas \
    -Dapp.pid=63123 \
    -Dapp.repo=lib \
    -Dapp.home=. \
    -Dbasedir=. \
    com.ecg.replyts.core.Application \
    2>&1 &

    echo $! > ${COMAAS_PID}

    log "Comaas pid: $(cat ${COMAAS_PID})")
}

function stopComaas() {
    if [[ -f ${COMAAS_PID} ]]; then
        PID=$(cat ${COMAAS_PID})
        log "Stopping comaas with PID $PID"
        kill -0 ${PID} 2>&1 >/dev/null || { log "Comaas already stopped"; return; }
        kill -9 ${PID}
        log "Comaas stopped"
    fi
}

trap "stopAll" EXIT TERM
function stopAll() {
    stopComaas
}

function main() {
    log "Packaging Comaas (-T ${TENANT} -P docker)"
    bin/build.sh -T ${TENANT} -P docker

    log "Importing properties into Consul"
    docker run --net ${DOCKER_NETWORK} --rm --volume ${PWD}/distribution/conf/${TENANT}/import_into_consul/docker.properties:/docker.properties -w / docker-registry.ecg.so/comaas/properties-to-consul:0.0.4 -consul http://${DOCKER_CONSUL}:8500 -file /docker.properties

    log "Starting comaas for tenant ${TENANT}"

    local start=$(date +"%s")

    startComaas

    for i in $(seq 1 ${ATTEMPTS}) ; do
       sleep "$HEALTH_CHECK_DELAY"
       log "Waiting for comaas to start. Listening on port $COMAAS_HTTP_PORT for ${HEALTH_CHECK_DELAY}s"
       set +o errexit
       HEALTH=$(curl -s http://localhost:${COMAAS_HTTP_PORT}/health)
       set -o errexit

       if [ ! -z "$HEALTH" ]; then
           echo "Comaas's health is $HEALTH"
       break
       fi

       if [ $i -eq ${ATTEMPTS} ]; then
          echo "Unable to get health from http://localhost:${COMAAS_HTTP_PORT}/health, exiting"
          exit 1
       fi
    done

    ACTUAL_TENANT=$(echo ${HEALTH} | jq -r ".tenant")
    if [ "$ACTUAL_TENANT" = "$TENANT" ] ; then
       echo "Host localhost is running the correct tenant: $ACTUAL_TENANT"
    else
       echo "Host localhost is NOT running the correct tenant: $ACTUAL_TENANT (should be running $TENANT)"
       exit 1
    fi

    stopComaas

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

function usage() {
    cat << EOF
    Usage:
    Run Comaas for TENANT optionally supplying docker network (default - 'comaasdocker_default')

    Examples:
    $0 mp
    $0 mp yourname_default

EOF
    exit 0;
}

parseCmd ${ARGS}
main
