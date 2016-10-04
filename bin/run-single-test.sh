#!/usr/bin/env bash
#
# run-single-test.sh runs a single test, provided on the command line.
# Example: './run-single-test.sh UserfilterIntegrationTest'

set -o nounset
set -o errexit

readonly ARGS="$@"
readonly DIR=$(dirname $0)

readonly CASSANDRA_DIR="$DIR/../cassandra_tmp"
readonly CASSANDRA_PID="cassandra.pid"
readonly CASSANDRA_HOME=$CASSANDRA_DIR


# Import a few certificates if we haven't already
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
    export PATH=$PATH:/opt/cassandra/bin:/usr/sbin
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

startCassandra
mvn -Drevision=123 -Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.trustStorePassword=comaas  -Dlogback.debug=true -U -s etc/settings.xml -T0.5C -am -DfailIfNoTests=false -P 'core,core-tests,!distribution'  clean package -Dtest=$@
