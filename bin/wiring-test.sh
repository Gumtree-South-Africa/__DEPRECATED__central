#!/usr/bin/env bash

# Adjustment of run-locally.sh script for new Jenkins (extracting environment start/stop to pipeline)
# This script starts Comaas with the "bare" profile, then checks the health to see if all beans are wired correctly.

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly COMAAS_PID="$PWD/comaas.pid"
readonly COMAAS_OUT="$PWD/comaas.out"

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
    log "Writing comaas output to ${COMAAS_OUT}"
    COMAAS_HTTP_PORT=$(findOpenPort)
    log "Starting comaas on port $COMAAS_HTTP_PORT"

    (cd distribution/target
    tar xfz distribution-${TENANT}-bare.tar.gz
    cd distribution

    COMAAS_HTTP_PORT=${COMAAS_HTTP_PORT} bin/comaas > ${COMAAS_OUT} 2>&1 &
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
    log "Packaging Comaas (-T ${TENANT} -P bare)"
    bin/build.sh -T ${TENANT} -P bare

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
    Run Comaas for TENANT

    Example:
    $0 mp

EOF
    exit 0;
}

parseCmd ${ARGS}
main
