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
readonly DIR=$(dirname $0)

CASSANDRA_CONTAINER_PORT=9042 # this will be overwritten by the Docker container port number

REVISION="$(git rev-parse --short HEAD)"
# Override REVISION in case of an in-progress Gerrit review
if [[ $(git rev-parse --abbrev-ref HEAD) == review* ]]; then
    REVISION="gerrit-$(git rev-parse --abbrev-ref HEAD | egrep -o '/[^/]+$' | egrep -o '[a-zA-Z0-9\-_]+')"
fi
log "Building revision $REVISION"

# Import a few certificates if we haven't already

MVN_ARGS="-Drevision=$REVISION -Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.trustStorePassword=comaas -U"

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

CASSANDRA_CONTAINER_NAME="not_started"
function startCassandra() {
    hash docker 2>/dev/null || fatal "I require docker but it's not installed. Aborting. More information: https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/blob/master/README.md"

    # stop & clean cassandra dir on exit
    trap "stopCassandra" EXIT

    CASSANDRA_CONTAINER_NAME=cassandra_test_${TENANT}_$(date +'%s')

    log "Starting cassandra: ${CASSANDRA_CONTAINER_NAME}"
    docker run --detach --publish-all --name ${CASSANDRA_CONTAINER_NAME} cassandra:2.1.14
    CASSANDRA_CONTAINER_PORT=$(docker port ${CASSANDRA_CONTAINER_NAME} 9042 | cut -d: -f2)
    log "Cassandra started on port ${CASSANDRA_CONTAINER_PORT}"
}

function stopCassandra() {
    set +o errexit
    docker top ${CASSANDRA_CONTAINER_NAME} 1>/dev/null 2>&1
    local ec=$?
    set -o errexit
    if [ ${ec} -eq 0 ]; then
      log "Stopping cassandra: "
      docker rm -fv ${CASSANDRA_CONTAINER_NAME}
    fi
}

function parseCmd() {
    RUN_TESTS=0
    RUN_INTEGRATION_TESTS=0
    RUN_ONLY_INTEGRATION_TESTS_P1=0
    RUN_ONLY_INTEGRATION_TESTS_P2=0
    RUN_CORE_TESTS=0
    TENANT=
    TENANT_ONLY=
    PACKAGE=
    EXECUTE=

    while getopts ":htI123T:R:P:E" OPTION; do
        case ${OPTION} in
            h) usage; exit 0;
               ;;
            t) log "Building with tests (but not integration tests)"; RUN_TESTS=1; RUN_CORE_TESTS=1
               ;;
            I) log "Building with tests and integration tests"; RUN_TESTS=1;  RUN_CORE_TESTS=1; RUN_INTEGRATION_TESTS=1
               ;;
            1) log "Building with integration tests part1 only"; RUN_TESTS=1; RUN_ONLY_INTEGRATION_TESTS_P1=1
               ;;
            2) log "Building with integration tests part2 only"; RUN_TESTS=1; RUN_ONLY_INTEGRATION_TESTS_P2=1
               ;;
            3) log "Building with core module tests only"; RUN_TESTS=1; RUN_CORE_TESTS=1
               ;;
            T) log "Building for tenant $OPTARG"; TENANT="$OPTARG"
               ;;
            R) log "Building and testing for tenant $OPTARG"; TENANT="$OPTARG"; TENANT_ONLY=1; RUN_TESTS=1;
               ;;
            P) log "Build and Package tenant $OPTARG"; PACKAGE="$OPTARG"
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
    if [[ ! -z $TENANT_ONLY && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you want to build and test tenant's code"
    fi
    if [[ ! -z $EXECUTE && -z $TENANT ]] ; then
        fatal "Must specify a tenant if you want to run Comaas"
    fi
}

function main() {
    local start=$(date +"%s")

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T0.5C"
    MVN_TASKS="clean compile test-compile"
    PROFILES=""

    # skip tests and set concurrency based on whether tests should be run
    if [[ ${RUN_TESTS} -eq 1 ]]; then
        startCassandra
        MVN_ARGS="$MVN_ARGS"
        MVN_TASKS="clean package"

        if [[ "$RUN_CORE_TESTS" -eq 1 ]] ; then
                PROFILES="core,core-tests,"
        fi
    else
        log "Skipping the tests"
        MVN_ARGS="$MVN_ARGS -DskipTests=true"
    fi

    if [[ "$RUN_INTEGRATION_TESTS" -eq 1 ]] ; then
        PROFILES="${PROFILES}integration-tests-part1,integration-tests-part2,"
    fi

    if ! [[ -z $TENANT ]] ; then
        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}"

        if ! [[ -z $PACKAGE ]] ; then
            MVN_ARGS="${MVN_ARGS} -Denv-name=${PACKAGE}"
            MVN_TASKS="clean package"
        fi

        if ! [[ -z $EXECUTE ]] ; then
            if [[ -z $PACKAGE ]]; then
                    PACKAGE="local"
            fi
            MVN_ARGS="${MVN_ARGS} -DconfDir=distribution/conf/${TENANT}/${PACKAGE} -DlogDir=/tmp
            -Dmail.mime.parameters.strict=false -Dmail.mime.address.strict=false
            -Dmail.mime.ignoreunknownencoding=true
            -Dmail.mime.uudecode.ignoreerrors=true -Dmail.mime.uudecode.ignoremissingbeginend=true
            -Dmail.mime.multipart.allowempty=true -Dmaven.test.skip=true -Dmaven.exec.skip=false"
            MVN_TASKS="clean verify"
        fi

    elif [[ "$RUN_ONLY_INTEGRATION_TESTS_P1" -eq 1 ]] ; then
        log "Running Integration Tests P1 only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P integration-tests-part1,!distribution "
        MVN_TASKS="clean package"
    elif [[ "$RUN_ONLY_INTEGRATION_TESTS_P2" -eq 1 ]] ; then
        log "Running Integration Tests P2 only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P integration-tests-part2,!distribution "
        MVN_TASKS="clean package"
    elif [[ "$RUN_CORE_TESTS" -eq 1 && "$RUN_INTEGRATION_TESTS" -eq 0 ]] ; then
        log "Running Core Tests only"
        MVN_ARGS="$MVN_ARGS -am -DfailIfNoTests=false -P core,core-tests,!distribution "
        MVN_TASKS="clean package"
    else
        log "Building all tenant modules (skipping distribution)"

        # Extract all tenant profile IDs from the POM, as build profile selection is limited (MNG-3328 etc.)
        TENANT=`
          sed -n '/<profile>/{n;s/.*<id>\(.*\)<\/id>/\1/;p;}' ${DIR}/../pom.xml | \
          grep -v 'default\|distribution\|deploy' | \
          tr $'\n' ','`

        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}!distribution"
    fi

    CMD="mvn ${MVN_ARGS} ${MVN_TASKS} -DtestLocalCassandraPort=${CASSANDRA_CONTAINER_PORT}"
    log "Executing: ${CMD}"
    ${CMD}

    stopCassandra

    local end=$(date +"%s")
    local diff=$(($end-$start))
    local time=$(printf "Total time: %d min %d sec" $(($diff / 60)) $(($diff % 60)))
    log ${time}
}

function usage() {
cat << EOF
Usage:
    no args (default) - build all modules
    -t - builds and tests (but not integration tests) for Comaas and tenants
    -I - builds and runs ALL tests (including integration tests)
    -1 - builds and runs part 1 integration tests
    -2 - builds and runs part 2 integration tests
    -3 - builds and runs core tests
    -t -T <TENANT> - build tenant's code and runs tests for all tenant's modules (including core modules)
    -R <TENANT> - build tenant's code and runs tests for tenant modules only (excluding core modules)
    -T <TENANT> - build tenant's code
    -T <TENANT> -P <ENVNAME> - build and package tenant's code for ENVNAME
    -T <TENANT> -E - build, package and execute tenant's code
    -T <TENANT> -P <ENVNAME> -E - build, package and execute tenant's code with ENVNAME

    where TENANT is one or more of [ebayk,mp,kjca,mde,gtau],
    ENVNAME is the properties profile name. common values [local, comaasqa, bare]

    Examples: "$0 -t -T ebayk,mp " - build and test ebayk and mp distributions

EOF
}

parseCmd ${ARGS}
main
