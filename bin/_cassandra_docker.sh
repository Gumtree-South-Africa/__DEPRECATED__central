#!/usr/bin/env bash

if [ -z ${TENANT+x} ]; then
    TENANT="none"
fi

readonly CASSANDRA_IMAGE_NAME="registry.ecg.so/comaas_cassandra_data:0.0.4"
readonly CASSANDRA_CONTAINER_NAME="cassandra_test_${TENANT//,/-}_$(date +'%s')"

# This value will be overwritten when the container is started
CASSANDRA_CONTAINER_PORT=9042

function startCassandra() {
    hash docker 2>/dev/null || fatal "I require docker but it's not installed. Aborting. More information: https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/blob/master/README.md"

    log "Starting cassandra: ${CASSANDRA_CONTAINER_NAME}"
    docker run --detach --publish-all --name ${CASSANDRA_CONTAINER_NAME} ${CASSANDRA_IMAGE_NAME}
    CASSANDRA_CONTAINER_PORT=$(docker port ${CASSANDRA_CONTAINER_NAME} 9042 | cut -d: -f2)
    log "Cassandra started on port ${CASSANDRA_CONTAINER_PORT} with name ${CASSANDRA_CONTAINER_NAME}"
}

# Don't forget to add stopCassandra to your trap "xxxx" EXIT statement.
function stopCassandra() {
    log "Stopping cassandra container '${CASSANDRA_CONTAINER_NAME}': "
    set +o errexit
    docker top ${CASSANDRA_CONTAINER_NAME} 1>/dev/null 2>&1
    local ec=$?
    set -o errexit
    if [ ${ec} -eq 0 ]; then
        log "Stopping cassandra: "
        docker rm -fv ${CASSANDRA_CONTAINER_NAME}
    else
        log "Cassandra was not running."
    fi
}
