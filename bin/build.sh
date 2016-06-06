#!/usr/bin/env bash
#
# build.sh builds comaas and optionally runs tests, packages, uploads and deploys

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)

readonly CASSANDRA_DIR="$DIR/../cassandra_tmp"
readonly CASSANDRA_PID="cassandra.pid"
readonly CASSANDRA_HOME=$CASSANDRA_DIR

REVISION="$(git rev-parse --short HEAD)"

# Override REVISION in case of an in-progress Gerrit review
if [[ $(git rev-parse --abbrev-ref HEAD) == review* ]]; then
    REVISION="gerrit-$(git rev-parse --abbrev-ref HEAD | egrep -o '/[^/]+$' | egrep -o '[0-9]+')"
fi

# Import a few certificates if we haven't already

MVN_ARGS="-Drevision=$REVISION -Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.trustStorePassword=comaas"

if [ ! -f comaas.jks ] ; then
    keytool -genkey -alias comaas -keyalg RSA -keystore comaas.jks -keysize 2048 \
      -dname "CN=com, OU=COMaaS, O=eBay Classifieds, L=Amsterdam, S=Noord-Holland, C=NL" \
      -storepass 'comaas' -keypass 'comaas'

    # autodeploy, kautodeploy, nexus.corp.mobile.de

    for HOST in autodeploy.corp.mobile.de kautodeploy.corp.mobile.de nexus.corp.mobile.de ; do
        openssl s_client -connect ${HOST}:443 </dev/null | \
          sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' 1>deploy.crt

        keytool -import -noprompt -trustcacerts -alias ${HOST} -file deploy.crt \
                -keystore comaas.jks -storepass 'comaas'

        rm -f deploy.crt
    done
fi

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

function startCassandra() {
    # stop & clean cassandra dir on exit
    trap "stopCassandra" EXIT

    log "Starting cassandra"
    rm -rf ${CASSANDRA_DIR}
    mkdir ${CASSANDRA_DIR}
    export PATH=$PATH:/opt/cassandra/bin:/usr/sbin
    cassandra "-Dcassandra.logdir=$CASSANDRA_DIR" "-Dcassandra.config=file:///$PWD/etc/cassandra.yaml" "-Dcassandra.storagedir=$CASSANDRA_DIR" -p ${CASSANDRA_PID}
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
            exit 1
        fi
        rm -rf ${CASSANDRA_DIR}
        rm -rf ${CASSANDRA_PID}
        log "Cassandra stopped"
    fi
}

function parseCmd() {
    RUN_TESTS=0
    RUN_INTEGRATION_TESTS=0
    RUN_ONLY_INTEGRATION_TESTS_ALL=0
    RUN_ONLY_INTEGRATION_TESTS_P1=0
    RUN_ONLY_INTEGRATION_TESTS_P2=0
    RUN_MP_INTEGRATION_TESTS=0
    TENANT=
    PACKAGE=
    UPLOAD=
    EXECUTE=

    while getopts ":tIi123T:P:U:E" OPTION; do
        case ${OPTION} in
            t) log "Building with tests (but not integration tests)"; RUN_TESTS=1
               ;;
            I) log "Building with tests and integration tests"; RUN_TESTS=1; RUN_INTEGRATION_TESTS=1
               ;;
            i) log "Building with all integration tests only"; RUN_TESTS=1; RUN_ONLY_INTEGRATION_TESTS_ALL=1
               ;;
            1) log "Building with integration tests part1 only"; RUN_TESTS=1; RUN_ONLY_INTEGRATION_TESTS_P1=1
               ;;
            2) log "Building with integration tests part2 only"; RUN_TESTS=1; RUN_ONLY_INTEGRATION_TESTS_P2=1
               ;;
            3) log "Building with MP integration tests only"; RUN_TESTS=1; RUN_MP_INTEGRATION_TESTS=1
               ;;
            T) log "Building for tenant $OPTARG"; TENANT="$OPTARG"
               ;;
            P) log "Build and Package tenant $OPTARG"; PACKAGE="$OPTARG"
               ;;
            U) log "Will upload to $OPTARG"; UPLOAD="$OPTARG"
               ;;
            E) log "Build and Execute Comaas for specific tenant. Please start ecg-comaas-vagrant manually"; EXECUTE=true
               ;;
            \?) fatal "Invalid option: -${OPTARG}"
               ;;
        esac
    done

    if [[ ! -z $PACKAGE && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you want to package"
    fi
    if [[ ! -z $UPLOAD && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you are specifying an upload target environment"
    fi
    if [[ ! -z $EXECUTE && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you want to run Comaas"
    fi
}

function main() {
    local start=$(date +"%s")

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T0.5C"
    MVN_TASKS="clean compile"

    # skip tests and set concurrency based on whether tests should be run
    if ! [[ ${RUN_TESTS} -eq 1 ]]; then
        log "Skipping the tests"
        MVN_ARGS="$MVN_ARGS -DskipTests=true"
    else
        startCassandra
        MVN_ARGS="$MVN_ARGS"
        MVN_TASKS="clean package"
    fi

    PROFILES=""

    if [ "$RUN_INTEGRATION_TESTS" -eq 0 ] ; then
        PROFILES="skip-integration-tests,"
    fi

    if ! [[ -z $TENANT ]] ; then
        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}"

        # Remove default profiles if supplied
        if [[ "$PACKAGE" == 'local' ]]; then
                PACKAGE=""
        fi
        if [[ "$UPLOAD" == 'local' ]]; then
                UPLOAD=""
        fi

        if ! [[ -z $PACKAGE ]] ; then
            MVN_ARGS="${MVN_ARGS} -Denv-name=${PACKAGE}"
            MVN_TASKS="package"
        fi

        if ! [[ -z $UPLOAD ]] ; then
            MVN_ARGS="${MVN_ARGS} -Denv-name=${UPLOAD}"
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

    elif [[ "$RUN_ONLY_INTEGRATION_TESTS_ALL" -eq 1 ]] ; then
        echo "Running All Integration Tests only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P mp,integration-tests-part1,integration-tests-part2 -pl integration-tests/mp-integration-test,integration-tests/core-integration-test "
        MVN_TASKS="clean package"
    elif [[ "$RUN_ONLY_INTEGRATION_TESTS_P1" -eq 1 ]] ; then
        echo "Running Integration Tests P1 only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P integration-tests-part1 -pl integration-tests/core-integration-test "
        MVN_TASKS="clean package"
    elif [[ "$RUN_MP_INTEGRATION_TESTS" -eq 1 ]] ; then
        echo "Running MP Integration Tests only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P mp -pl integration-tests/mp-integration-test "
        MVN_TASKS="clean package"
    elif [[ "$RUN_ONLY_INTEGRATION_TESTS_P2" -eq 1 ]] ; then
        echo "Running Integration Tests P2 only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P integration-tests-part2 -pl integration-tests/core-integration-test "
        MVN_TASKS="clean package"
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

    stopCassandra

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

parseCmd ${ARGS}
main
