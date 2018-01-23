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
    # Mac (Darwin) does not have '--no-run-if-empty' option for xargs and work fine with empty lists
    if [ "$(uname -s)" == "Darwin" ]
    then
       docker ps --format {{.Names}} | grep comaas | xargs docker rm --force --volumes
    else
       docker ps --format {{.Names}} | grep comaas | xargs --no-run-if-empty docker rm --force --volumes
    fi
}

function startEnv() {
    cleanEnv
    if [ ! -f ${DOCKER_COMPOSE_FILE} ]; then
        echo "ERROR: ${DOCKER_COMPOSE_FILE} was not found, could not start test environment therefore, exiting"
        exit
    fi

    set +e
    ${DOCKER_COMPOSE} up -d
    success=$?
    if [ $success -ne 0 ]; then
        echo
        echo "Did you try logging in?"
        echo "docker login docker-registry.ecg.so"
        echo
        exit $success
    fi
    set -e
}

function stopEnv() {
    ${DOCKER_COMPOSE} down --remove-orphans --volumes
}
