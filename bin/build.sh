#!/usr/bin/env bash
#
# build.sh builds comaas and optionally runs tests, packages, uploads and deploys

set -o nounset
set -o errexit

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

readonly ARGS="$@"
export DIR=$(dirname $0)

MVN_ARGS="--update-snapshots"

function parseCmd() {
    UNIT_TESTS=0
    INTEGRATION_TESTS=0
    PACKAGE=0

    while getopts ":htip" OPTION; do
        case ${OPTION} in
            h) usage; exit 0;
               ;;
            t) log "Run unit tests"; UNIT_TESTS=1;
               ;;
            i) log "Run integration tests"; INTEGRATION_TESTS=1
               ;;
            p) log "Package"; PACKAGE=1;
               ;;
            \?) fatal "Invalid option: -${OPTARG}"
               ;;
        esac
    done

    if [ $(uname) == "Linux" ]; then
        log "Building on host $(hostname -A)"
    fi
}

function main() {
    source "${DIR}/docker-test-env.sh"
    local start=$(date +"%s")

    # export region as on salt-managed environments
    export region=localhost

    MVN_TASKS="clean test -Dskip.surefire.tests"
    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T0.5C"
    PROFILES=""

    # skip tests and set concurrency based on whether tests should be run
    if [[ ${UNIT_TESTS} -eq 1 ]]; then
        MVN_TASKS="clean test"
        MVN_ARGS="$MVN_ARGS"
    fi

    if [[ "$INTEGRATION_TESTS" -eq 1 ]] ; then
        startEnv
        trap "stopEnv" EXIT TERM

        MVN_TASKS="clean verify"
        MVN_ARGS="$MVN_ARGS -Dskip.surefire.tests"
    fi

    if [[ "$PACKAGE" -eq 1 ]] ; then
        MVN_TASKS="clean package"
        MVN_ARGS="$MVN_ARGS -Dskip.surefire.tests -Pdistribution -pl distribution -am"
    fi

    MVN_CMD="mvn"

    export COMAAS_HTTP_PORT=18081
    export COMAAS_HAZELCAST_IP=127.0.0.1
    export tenant=${TENANT}
    CMD="${MVN_CMD} ${MVN_ARGS} ${MVN_TASKS}"
    log "Executing: ${CMD}"
    ${CMD}

    stopEnv

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

function usage() {
cat << EOF
Usage:
    no args (default) - build all modules
    -t - builds and runs unit tests
    -i - builds and runs integration tests
    -p - builds and packages
EOF
}

parseCmd ${ARGS}
main
