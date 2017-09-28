#!/usr/bin/env bash

# Source this file to start a prod-like environment

if [ -z ${TENANT+x} ]; then
    export TENANT="none"
else
    export TENANT
fi

readonly CREATION_SECONDS="$(date +'%s')"
readonly PROJECT_NAME="comaasZtestZ${TENANT}Z${CREATION_SECONDS}"
readonly COMPOSE_DIR="$(mktemp -d)"
# Note that dashes and underscores are not allowed in the project name... which is why there is this Z separator

# Check if there is any process on the Cassandra port, and fail if there is
function checkEnv() {
    set +o errexit
    nc -w1 localhost 9042 > /dev/null
    local s=$?
    set -o errexit
    if [ ${s} -eq 0 ]; then
        echo "There is already a running process on port 9042, cannot continue."
        exit 1
    fi
}

function cleanEnv() {
    # Remove any "comaasz" containers left over by a previous run
    docker ps -a | grep comaasz | awk '{print $1}' | xargs --no-run-if-empty docker rm -fv
}

function startEnv() {
    cleanEnv
    checkEnv
    rm -rf ${COMPOSE_DIR}
    git clone --single-branch --depth 1 git@github.corp.ebay.com:ecg-comaas/ecg-comaas-docker.git ${COMPOSE_DIR}
    (cd ${COMPOSE_DIR} && ECG_COMAAS_DOCKER_PROJECT_NAME=${PROJECT_NAME} make up)
}

function stopEnv() {
    if [ -d ${COMPOSE_DIR} ]; then
        (cd ${COMPOSE_DIR} && ECG_COMAAS_DOCKER_PROJECT_NAME=${PROJECT_NAME} make down)
        rm -rf ${COMPOSE_DIR}
    fi
}
