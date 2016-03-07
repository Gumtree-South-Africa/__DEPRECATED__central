#!/bin/bash

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)

# ignore SSL warnings so that we don't have to import tenant repository certificates

MVN_ARGS="-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

function parseCmd() {
    RUN_TESTS=0
    RUN_INTEGRATION_TESTS=0
    TENANT=

    while getopts ":tIT:" OPTION; do
        case ${OPTION} in
            t) log "Building with tests (but not integration tests)"; RUN_TESTS=1
               ;;
            I) log "Building with tests and integration tests"; RUN_TESTS=1; RUN_INTEGRATION_TESTS=1
               ;;
            T) log "Building for tenant $OPTARG"; TENANT="$OPTARG"
               ;;
            \?) fatal "Invalid option: -${OPTARG}"
               ;;
        esac
    done
}

function main() {
    local start=$(date +"%s")

    # we would use -T1C (one thread per core), but this breaks tests that start an embedded Cassandra instance
    # so for now we run with 1 thread.

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T1 clean package"

    if ! [[ ${RUN_TESTS} -eq 1 ]]; then
        log "Skipping the tests"

        MVN_ARGS="$MVN_ARGS -DskipTests=true"
    fi

    PROFILES=""

    if [ "$RUN_INTEGRATION_TESTS" -eq 0 ] ; then
        PROFILES="skip-integration-tests,"
    fi

    if ! [[ -z "$TENANT" ]]; then
        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}"
    else
        log "Building all tenant modules (skipping distribution)"

        # Extract all tenant profile IDs from the POM, as build profile selection is limited (MNG-3328 etc.)

        TENANT=`
          sed -n '/<profile>/{n;s/.*<id>\(.*\)<\/id>/\1/;p;}' $DIR/../pom.xml | \
          grep -v 'default\|distribution' | \
          tr $'\n' ','`

        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}!distribution"
    fi

    mvn $MVN_ARGS

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

parseCmd ${ARGS}
main
