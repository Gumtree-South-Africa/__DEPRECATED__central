#!/usr/bin/env bash

if [ -z ${TENANT+x} ]; then
    export TENANT="none"
else
    export TENANT
fi

readonly DOCKER_COMPOSE_FILE="${DIR}/conf/docker-compose.yml"
readonly DOCKER_COMPOSE="docker-compose -f ${DOCKER_COMPOSE_FILE}"

function startEnv() {
    if [ ! -f ${DOCKER_COMPOSE_FILE} ]; then
        echo "ERROR: ${DOCKER_COMPOSE_FILE} was not found, could not start test environment therefore, exiting"
        exit
    fi
    export CREATION_SECONDS="$(date +'%s')"

    ${DOCKER_COMPOSE} up -d
}

function stopEnv() {
    ${DOCKER_COMPOSE} down --remove-orphans --volumes
}
