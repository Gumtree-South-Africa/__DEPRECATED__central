#!/usr/bin/env bash
#
# build.sh builds comaas and optionally runs tests, packages, uploads and deploys

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)
readonly CASSANDRA_DIR="$DIR/../cassandra_tmp"
readonly CASSANDRA_PID="cassandra.pid"

REVISION="$(git rev-parse --short HEAD)"

# Override REVISION in case of an in-progress Gerrit review

find .git/refs/heads/review -type f 2>/dev/null && \
  REVISION="gerrit-$(basename `find .git/refs/heads/review -type f`)"

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
    /opt/cassandra/bin/cassandra -p ${CASSANDRA_PID} "-Dcassandra.storagedir=$CASSANDRA_DIR"
}

function stopCassandra() {
    if [[ -e ${CASSANDRA_PID} ]]; then
        log "Stopping cassandra"
        kill $(cat ${CASSANDRA_PID})
        rm ${CASSANDRA_PID}
        rm -rf ${CASSANDRA_DIR}
    fi
}

function parseCmd() {
    RUN_TESTS=0
    RUN_INTEGRATION_TESTS=0
    TENANT=
    PACKAGE=
    UPLOAD=
    EXECUTE=

    while getopts ":tIT:P:U:E" OPTION; do
        case ${OPTION} in
            t) log "Building with tests (but not integration tests)"; RUN_TESTS=1
               ;;
            I) log "Building with tests and integration tests"; RUN_TESTS=1; RUN_INTEGRATION_TESTS=1
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
            MVN_ARGS="${MVN_ARGS},upload-${TENANT}-${PACKAGE}"
    	    MVN_TASKS="package"
        fi

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

    stopCassandra

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

parseCmd ${ARGS}
main
