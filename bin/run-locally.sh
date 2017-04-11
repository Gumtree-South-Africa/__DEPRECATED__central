#!/usr/bin/env bash

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly COMAAS_PID="$PWD/comaas.pid"
readonly COMAAS_OUT="$PWD/comaas.out"

readonly CASSANDRA_DIR="$PWD/../cassandra_tmp"
readonly CASSANDRA_PID="$PWD/cassandra.pid"
readonly CASSANDRA_HOME=$CASSANDRA_DIR
readonly ATTEMPTS=60
readonly HEALTH_CHECK_DELAY=3
readonly HOST='localhost'

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

function parseCmd() {
  # check amount of args
  [[ $# == 0 ]] && usage
  TENANT=$1
}

source "${DIR}/_cassandra_docker.sh"

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
    cd distribution/target
    tar xfz distribution-${TENANT}-bare.tar.gz
    cd distribution
    sed -i'.bak' "s/:9042$/:${CASSANDRA_CONTAINER_PORT}/" conf/replyts.properties

    log "Writing comaas output to ${COMAAS_OUT}"
    COMAAS_HTTP_PORT=$(findOpenPort)
    log "Starting comaas on port $COMAAS_HTTP_PORT"
    COMAAS_HTTP_PORT=${COMAAS_HTTP_PORT} bin/comaas > ${COMAAS_OUT} 2>&1 &
    echo $! > ${COMAAS_PID}

    log "Comaas pid: $(cat ${COMAAS_PID})"
}

function stopComaas() {
    if [[ -f ${COMAAS_PID} ]]; then
        log "Stopping comaas"
        PID=$(cat ${COMAAS_PID})
        kill -0 ${PID} 2>&1 >/dev/null || { log "Comaas already stopped"; return; }

        kill -9 ${PID}
        log "Comaas stopped"
    fi
}

trap "stopAll" EXIT
function stopAll() {
    stopCassandra
    stopComaas
}

function main() {
   log "Starting comaas for tenant $TENANT"
   local start=$(date +"%s")

   if [[ "$TENANT" == "mp" ]] || [[ "$TENANT" == "mde" ]] ; then
       startCassandra
       log "Waiting for Cassandra to become available"

       sleep 5 # give the cassandra container some time to settle

       for i in $(seq 1 ${ATTEMPTS}); do
         health=$(docker inspect --format "{{json .State.Health.Status }}" ${CASSANDRA_CONTAINER_NAME})
         if [[ ${health} == "\"healthy\"" ]]; then
            log "Cassandra is up"
            break
         fi
         sleep 1
       done
       if [[ ${i} -ge ${ATTEMPTS} ]]; then
          fatal "Cassandra took too long to start up. Failing."
       fi
   fi

   startComaas
   sleep 10

   for i in $(seq 1 $ATTEMPTS) ; do
       sleep "$HEALTH_CHECK_DELAY"
       log "Waiting for comaas to start. Listening on port $COMAAS_HTTP_PORT for $(($HEALTH_CHECK_DELAY * $i))s"
       set +o errexit
       HEALTH=$(curl -s http://${HOST}:${COMAAS_HTTP_PORT}/health)
       set -o errexit

       if [ ! -z "$HEALTH" ]; then
           echo "${HOST}'s health is $HEALTH"
	   break
       fi

       if [ $i -eq $ATTEMPTS ]; then
          echo "Unable to get health from http://${HOST}:${COMAAS_HTTP_PORT}/health, exiting"
          exit 1
       fi
   done

   ACTUAL_TENANT=$(echo ${HEALTH} | jq -r ".tenant")
   if [ "$ACTUAL_TENANT" = "$TENANT" ] ; then
       echo "Host ${HOST} is running the correct tenant: $ACTUAL_TENANT"
   else
       echo "Host ${HOST} is NOT running the correct version: $ACTUAL_TENANT (should be running $TENANT)"
       exit 1
   fi

   stopComaas

   if [[ "$TENANT" == "mp" ]] ; then
       stopCassandra
   fi

   local end=$(date +"%s")
   local diff=$(($end-$start))
   local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
   log ${time}
}

function usage() {
cat << EOF
Usage:
    Run Comaas for TENANT where TENANT one of ebayk,mp,mde,kjca,gtau,bt
    Make sure to create a 'bare' package first (e.g. ./bin/build.sh -T mp -P bare)

Example:
    $0 mp

EOF
exit 0;
}

parseCmd ${ARGS}
main

