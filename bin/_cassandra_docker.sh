#!/usr/bin/env bash
#TODO: to be removed when migrated to new Jenkins

if [ -z ${TENANT+x} ]; then
    TENANT="none"
fi

readonly CASSANDRA_IMAGE_NAME="docker-registry.ecg.so/comaas/cassandra_data:0.0.9"
readonly CASSANDRA_CONTAINER_NAME="cassandra_test_${TENANT//,/-}_pid$$_$(date +'%s')"

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
    stopCassandraWithContainerName ${CASSANDRA_CONTAINER_NAME}
}

function stopCassandraWithContainerName() {
    local containerName="$1"
    log "Stopping cassandra container '${containerName}': "
    set +o errexit
    docker top ${containerName} 1>/dev/null 2>&1
    local ec=$?
    set -o errexit
    if [ ${ec} -eq 0 ]; then
        log "Stopping cassandra: "
        docker rm -fv ${containerName}
    else
        log "Cassandra was not running."
    fi

}

function stopDanglingCassandras() {
    for cassandraOwnerPid in $(docker ps | grep 'cassandra_test_' | grep -Po "(?<=_pid)[0-9]+") ; do
        set +o errexit
        grep 'bin/build.sh' /proc/${cassandraOwnerPid}/cmdline
        local ec=$?
        set -o errexit
        if [ ${ec} -ne 0 ]; then
            log "Found an obsolete cassandra container for pid ${cassandraOwnerPid}, going to stop it..."
            stopCassandraWithContainerName $(docker ps | grep -Po "cassandra_test\S+_pid${cassandraOwnerPid}_\S+")
        fi
    done
}
