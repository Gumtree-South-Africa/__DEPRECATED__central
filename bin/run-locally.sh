#!/usr/bin/env bash

set -o nounset
#set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly COMAAS_PID="$PWD/comaas.pid"
readonly COMAAS_OUT="$PWD/comaas.out"

readonly CASSANDRA_DIR="$PWD/../cassandra_tmp"
readonly CASSANDRA_PID="$PWD/cassandra.pid"
readonly CASSANDRA_HOME=$CASSANDRA_DIR
readonly PORT=18081
readonly ATTEMPTS=10
readonly HOST='localhost'

function parseCmd() {
  # check amount of args
  [[ $# == 0 ]] && usage
  TENANT=$1
}

function startCassandra() {
    # stop & clean cassandra dir on exit
    trap "stopCassandra" EXIT
    export PATH=$PATH:/opt/cassandra/bin:/usr/sbin
    log "Starting cassandra"
    rm -rf ${CASSANDRA_DIR}
    mkdir ${CASSANDRA_DIR}
    export PATH=$PATH:/opt/cassandra/bin:/usr/sbin
    cassandra "-Dcassandra.logdir=$CASSANDRA_DIR" "-Dcassandra.config=file:///$PWD/etc/cassandra.yaml" "-Dcassandra.storagedir=$CASSANDRA_DIR" -p ${CASSANDRA_PID} > cassandra.out 2>&1
}

function stopCassandra() {
    if [[ -e ${CASSANDRA_PID} ]]; then
        log "Stopping cassandra"
        PID=$(cat ${CASSANDRA_PID})
        kill ${PID}

        counter=0
        while $(ps -p "$PID" > /dev/null); do
            if [ ${counter} -gt 10 ]; then
                break
            fi
            log "waiting for Cassandra to exit"
            sleep 1
            counter=$(expr ${counter} + 1)
        done
        if $(ps -p "$PID" > /dev/null); then
            log "Cassandra didn't exit in 10 seconds"
        fi
        rm -rf ${CASSANDRA_DIR}
        rm -rf ${CASSANDRA_PID}
        log "Cassandra stopped"
    fi
}

function startComaas() {
    # stop & clean cassandra dir on exit
    trap "stopComaas" EXIT
    log "Starting Comaas"
    cd distribution/target
    tar xfz distribution-$TENANT-bare.tar.gz
    cd distribution
    echo "Writing comaas output to ${COMAAS_OUT}"
    bin/comaas > ${COMAAS_OUT} 2>&1 &
    echo $! > ${COMAAS_PID}
    CPID=`cat ${COMAAS_PID}`
    log "Comaas pid: $CPID"
}

function stopComaas() {
    if [[ -f ${COMAAS_PID} ]]; then
        log "Stopping comaas"
        PID=$(cat ${COMAAS_PID})
        kill ${PID}

        counter=0
        while $(ps -p "$PID" > /dev/null); do
            if [ ${counter} -gt 10 ]; then
                break
            fi
            log "waiting for Comaas to exit"
            sleep 1
            counter=$(expr ${counter} + 1)
        done
        if $(ps -p "$PID" > /dev/null); then
            log "Comaas didn't exit in 10 seconds, killing it. You may want to check if cassandra is still running"
            kill ${PID}
        fi
        rm ${COMAAS_PID}
        log "Comaas stopped"
    fi
}

function log() {
    echo "[$(date)]: $*"
}

function main() {
   log "Starting comaas for tenant $TENANT"
   local start=$(date +"%s")
   startCassandra
   sleep 5
   $DIR/setup-cassandra.sh localhost replyts2

   startComaas
   sleep 6


   for i in $(seq 1 $ATTEMPTS) ; do
       sleep 2
       HEALTH=$(curl -s http://${HOST}:${PORT}/health)
       echo "${HOST}'s health is $HEALTH"

       if [ -n "$HEALTH" ]; then
           log "Waiting for comaas to start."
           break
       fi

       if [ $i -eq $ATTEMPTS ]; then
          echo "Unable to get health from http://${HOST}:${PORT}/health, exiting"

       fi
   done

   ACTUAL_TENANT=$(echo ${HEALTH} | jq -r ".tenant")
   if [ "$ACTUAL_TENANT" = "$TENANT" ] ; then
       echo "Host ${HOST} is running the correct tenant: $ACTUAL_TENANT"
   else
       echo "Host ${HOST} is NOT running the correct version: $ACTUAL_TENANT (should be running $TENANT)"
   fi

   stopComaas
   stopCassandra
   sleep 3

   local end=$(date +"%s")
   local diff=$(($end-$start))
   local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
   log ${time}
}

function usage() {
cat << EOF
Usage:
    no args (default) - build all modules
    -t - builds and tests (but not integration tests) for Comaas and tenants

    Examples: "$0 -t -T ebayk,mp " - build and test ebayk and mp distributions

EOF
}

parseCmd ${ARGS}
main