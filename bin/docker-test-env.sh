#!/usr/bin/env bash

if [ -z ${TENANT+x} ]; then
    export TENANT="none"
else
    export TENANT
fi

readonly DOCKER_COMPOSE_FILE="${DIR}/conf/docker-compose.yml"
readonly CREATION_SECONDS="$(date +'%s')"
readonly DOCKER_PROJECT_NAME="comaasZtestZ${TENANT}Z${CREATION_SECONDS}"
readonly DOCKER_COMPOSE="docker-compose -f ${DOCKER_COMPOSE_FILE} --project-name ${DOCKER_PROJECT_NAME}"

function cleanEnv() {
     OLD_CONTAINERS="$(docker ps --format {{.Names}} | grep comaas || true)"
     [ -z "$OLD_CONTAINERS" ] || docker rm --force --volumes $OLD_CONTAINERS
}

function startEnv() {
    cleanEnv
    if [ ! -f ${DOCKER_COMPOSE_FILE} ]; then
        echo "ERROR: ${DOCKER_COMPOSE_FILE} was not found, could not start test environment therefore, exiting"
        exit
    fi

    set +e
    ${DOCKER_COMPOSE} up -d
    sleep 10
    success=$?
    if [ $success -ne 0 ]; then
        echo
        echo "Did you try logging in?"
        echo "docker login dock.es.ecg.tools"
        echo
        exit $success
    fi
    set -e
}

function stopEnv() {
    ${DOCKER_COMPOSE} down --remove-orphans --volumes
}
