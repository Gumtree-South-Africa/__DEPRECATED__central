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

# If we are running on a non-builder environment (locally for example) then import the certificates into
# a local trust store; on builder we rely on e.g. mobile-ca-certificates being installed

if [ ! -f /usr/bin/apt-get ] ; then
    MVN_ARGS="-Djavax.net.ssl.trustStore=${PWD}/comaas.jks -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.trustStorePassword=comaas $MVN_ARGS"

    curl -s0 -o comaas.jks -z comaas.jks https://swift.dus1.cloud.ecg.so/v1/81776e54d8664296b0fab63916911ca3/public/java/keystore/comaas.jks
else
    # If we are running on a builder environment then the certificates should have been installed in
    # /etc/ssl/certs/java/cacerts (mobile-ca-certificates doesn't support Oracle JDK)

    MVN_ARGS="-Djavax.net.ssl.trustStore=/etc/java-8-oracle/security/cacerts -Djavax.net.ssl.trustStorePassword=changeit $MVN_ARGS"
fi

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
    WAIT_FOR_DEBUGGER=0

    while getopts ":htI123T:R:P:ED" OPTION; do
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
            R) log "Building and testing for tenant modules $OPTARG"; TENANT="$OPTARG"; TENANT_ONLY=1; RUN_TESTS=1;
               ;;
            P) log "Build and Package profile $OPTARG"; PACKAGE="$OPTARG"
               ;;
            E) log "Build and Execute Comaas for specific tenant. Please start ecg-comaas-docker manually"; EXECUTE=true
               ;;
            D) log "Wait for debugger before running Maven" ; WAIT_FOR_DEBUGGER=1;
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
    source "${DIR}/docker-test-env.sh"
    local start=$(date +"%s")

    # export region as on salt-managed environments
    export region=localhost
    export swift_authentication_url=https://keystone.ams1.cloud.ecg.so/v2.0

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T0.5C"
    MVN_TASKS="clean compile test-compile"
    PROFILES=""

    # skip tests and set concurrency based on whether tests should be run
    if [[ ${RUN_TESTS} -eq 1 ]]; then
        startEnv
        trap "stopEnv" EXIT TERM

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
                    PACKAGE="docker"
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
        log "Building all tenant modules (skipping distribution and benchmarks)"

        # Extract all tenant profile IDs from the POM, as build profile selection is limited (MNG-3328 etc.)
        TENANT=`
          sed -n '/<profile>/{n;s/.*<id>\(.*\)<\/id>/\1/;p;}' ${DIR}/../pom.xml | \
          grep -v 'default\|distribution\|deploy\|benchmarks' | \
          tr $'\n' ','`

        MVN_ARGS="$MVN_ARGS -P${PROFILES}${TENANT}!distribution"
    fi

    MVN_CMD=
    if [[ ${WAIT_FOR_DEBUGGER} -eq 1 ]]; then
        MVN_CMD="mvnDebug"
    else
        MVN_CMD="mvn"
    fi

    if [[ ! -z "$EXECUTE" && "$PACKAGE" == "docker" ]]; then
        log "Selected local docker profile, importing tenant's properties..."
        set +o errexit
        status=$(curl -so /dev/null -w "%{http_code}" localhost:8599/v1/status/leader)
        set -o errexit
        if [[ "$status" != "200" ]]; then
            log "Consul does not seem to be up. Cannot import properties. Exiting."
            log "Hint: (cd docker && make up)"
            exit 1
        fi
        docker run --net comaasdocker_default --rm --volume ${PWD}/distribution/conf/${TENANT}/import_into_consul/docker.properties:/docker.properties -w / docker-registry.ecg.so/comaas/properties-to-consul:0.0.4 -consul http://comaasdocker_consul_1:8500 -file /docker.properties
    fi

    export COMAAS_HTTP_PORT=18081
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
    -D - suspends Maven until a remote debugger is attached. May be combined with any of the options above

    where TENANT is one or more of [ebayk,mp,kjca,mde,gtau,bt,it],
    ENVNAME is the properties profile name. common values [local, comaasqa, bare]

    Examples: "$0 -t -T ebayk,mp " - build and test ebayk and mp distributions

EOF
}

parseCmd ${ARGS}
main
