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
    UPLOAD=
    EXECUTE=

    while getopts ":tIT:U:E" OPTION; do
        case ${OPTION} in
            t) log "Building with tests (but not integration tests)"; RUN_TESTS=1
               ;;
            I) log "Building with tests and integration tests"; RUN_TESTS=1; RUN_INTEGRATION_TESTS=1
               ;;
            T) log "Building for tenant $OPTARG"; TENANT="$OPTARG"
               ;;
            U) log "Will upload to $OPTARG"; UPLOAD="$OPTARG"
               ;;
            E) log "Build and Execute Comaas for specific tenant. Please start ecg-comaas-vagrant manually"; EXECUTE=true
               ;;
            \?) fatal "Invalid option: -${OPTARG}"
               ;;
        esac
    done

    if [[ ! -z $UPLOAD && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you are specifying an upload target environment"
    fi
    if [[ ! -z $EXECUTE && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you want to run Comaas"
    fi
}

function main() {
    local start=$(date +"%s")

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml"
    MVN_TASKS="clean compile"

    # skip tests and set concurrency based on whether tests should be run
    if ! [[ ${RUN_TESTS} -eq 1 ]]; then
        log "Skipping the tests"
        MVN_ARGS="$MVN_ARGS -T1C -DskipTests=true"
    else
        # we would use -T1C (one thread per core), but this breaks tests that start an embedded Cassandra instance
        # so for now we run with 1 thread.
        MVN_ARGS="$MVN_ARGS -T1"
        MVN_TASKS="clean package"
    fi

    PROFILES=""

    if [ "$RUN_INTEGRATION_TESTS" -eq 0 ] ; then
        PROFILES="skip-integration-tests,"
    fi

    if ! [[ -z $TENANT ]] ; then
        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}"

        if ! [[ -z $UPLOAD ]] ; then
            MVN_ARGS="${MVN_ARGS},upload-${TENANT}-${UPLOAD}"
	    MVN_TASKS="clean deploy"
        fi

        if ! [[ -z $EXECUTE ]] ; then
            MVN_ARGS="${MVN_ARGS} -DconfDir=distribution/conf/${TENANT}/local -DlogDir=/tmp
            -Dmail.mime.parameters.strict=false -Dmail.mime.address.strict=false
            -Dmail.mime.ignoreunknownencoding=true
            -Dmail.mime.uudecode.ignoreerrors=true -Dmail.mime.uudecode.ignoremissingbeginend=true
            -Dmail.mime.multipart.allowempty=true -Dmaven.test.skip=true -Dmaven.exec.skip=false"
	    MVN_TASKS="clean verify"
        fi
    else
        log "Building all tenant modules (skipping distribution)"

        # Extract all tenant profile IDs from the POM, as build profile selection is limited (MNG-3328 etc.)
        TENANT=`
          sed -n '/<profile>/{n;s/.*<id>\(.*\)<\/id>/\1/;p;}' $DIR/../pom.xml | \
          grep -v 'default\|distribution\|deploy' | \
          tr $'\n' ','`

        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}!distribution"
    fi

    log "Executing: mvn $MVN_ARGS $MVN_TASKS"
    mvn $MVN_ARGS $MVN_TASKS

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

parseCmd ${ARGS}
main
