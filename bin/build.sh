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
readonly CASSANDRA_IMAGE_NAME="docker-registry.ecg.so/comaas/cassandra_data:0.0.6"

REVISION="$(git rev-parse --short HEAD)"
# Override REVISION in case of an in-progress Gerrit review
if [[ $(git rev-parse --abbrev-ref HEAD) == review* ]]; then
    REVISION="gerrit-$(git rev-parse --abbrev-ref HEAD | egrep -o '/[^/]+$' | egrep -o '[a-zA-Z0-9_-]+')"
fi
log "Building revision $REVISION"

# Import a few certificates if we haven't already

MVN_ARGS="-Drevision=$REVISION -U"

# If we are running on a non-builder environment (locally for example) then import the certificates into
# a local trust store; on builder we rely on e.g. mobile-ca-certificates being installed

if [ ! -f /usr/bin/apt-get ] ; then
    MVN_ARGS="-Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.keyStoreType=JKS -Djavax.net.ssl.trustStorePassword=comaas $MVN_ARGS"

    if [ ! -f comaas.jks ] ; then
        keytool -genkey -alias comaas -keyalg RSA -keystore comaas.jks -keysize 2048 \
          -dname "CN=com, OU=COMaaS, O=eBay Classifieds, L=Amsterdam, S=Noord-Holland, C=NL" \
          -storepass 'comaas' -keypass 'comaas'

        # Install eBay SSL CA v2
        curl -sO "http://pki.corp.ebay.com/root-certs-pem.zip" && unzip root-certs-pem.zip

        for f in root-certs-pem/*.pem; do
            keytool -importcert -keystore comaas.jks -storepass 'comaas' -file ${f} -alias ${f} -noprompt
        done
        rm -rf root-certs-pem.zip root-certs-pem

 		# Install AMS1 & DUS1 CA
     	openssl s_client -connect keystone.ams1.cloud.ecg.so:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM |
          keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias ams1 -noprompt

        openssl s_client -connect keystone.dus1.cloud.ecg.so:443 -showcerts </dev/null 2>/dev/null | openssl x509 -outform PEM |
          keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias dus1 -noprompt

        # Install Gumtree AU nexus CA
        openssl s_client -showcerts -connect nexus.au.ecg.so:443 </dev/null 2>/dev/null | openssl x509 -outform PEM | \
          keytool -importcert -keystore comaas.jks -storepass 'comaas' -alias nexusau -noprompt
    fi
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
            E) log "Build and Execute Comaas for specific tenant. Please start ecg-comaas-vagrant manually"; EXECUTE=true
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
    source "${DIR}/_cassandra_docker.sh"
    local start=$(date +"%s")

    # export region as on salt-managed environments
    export region=localhost
    export swift_authentication_url=https://keystone.ams1.cloud.ecg.so/v2.0

    MVN_ARGS="$MVN_ARGS -s etc/settings.xml -T0.5C"
    MVN_TASKS="clean compile test-compile"
    PROFILES=""

    # skip tests and set concurrency based on whether tests should be run
    if [[ ${RUN_TESTS} -eq 1 ]]; then
        # A workaround for Jenkins builder nodes to clean up cassandra containers which weren't stopped due to failed builds
        # (for more examples of the following pattern see https://github.com/search?q=ugly+hack&ref=cmdform&type=Code )
        set +o nounset
        local SSH_CONNECTION="$SSH_CONNECTION"
        set -o nounset
        if [ -n "$SSH_CONNECTION" ] ; then
            stopDanglingCassandras
        fi
        startCassandra
        trap "stopCassandra" EXIT TERM

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

    MVN_CMD=
    if [[ ${WAIT_FOR_DEBUGGER} -eq 1 ]]; then
        MVN_CMD="mvnDebug"
    else
        MVN_CMD="mvn"
    fi

    if [[ ! -z "$EXECUTE" && "$PACKAGE" == "docker" ]]; then
        log "Selected local docker profile, importing tenant's properties..."
        set +o errexit
        status=$(curl -sIo /dev/null -w "%{http_code}" localhost:8500/v1/status/leader)
        set -o errexit
        if [[ "$status" != "200" ]]; then
            log "Consul does not seem to be up. Cannot import properties. Exiting."
            log "Hint: (cd docker && make up)"
            exit 1
        fi
        docker run --net comaasdocker_default --rm --volume ${PWD}/distribution/conf/${TENANT}/import_into_consul/docker.properties:/docker.properties -w / ${CASSANDRA_IMAGE_NAME} -consul http://comaasdocker_consul_1:8500 -file /docker.properties
    fi

    export COMAAS_HTTP_PORT=18081
    CMD="${MVN_CMD} ${MVN_ARGS} ${MVN_TASKS} -DtestLocalCassandraPort=${CASSANDRA_CONTAINER_PORT}"
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
    -D - suspends Maven until a remote debugger is attached. May be combined with any of the options above

    where TENANT is one or more of [ebayk,mp,kjca,mde,gtau,bt,it],
    ENVNAME is the properties profile name. common values [local, comaasqa, bare]

    Examples: "$0 -t -T ebayk,mp " - build and test ebayk and mp distributions

EOF
}

parseCmd ${ARGS}
main
