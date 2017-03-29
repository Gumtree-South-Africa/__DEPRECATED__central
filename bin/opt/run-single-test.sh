#!/usr/bin/env bash
#
# run-single-test.sh runs a single test, provided on the command line.
# Example: 'bin/run-single-test.sh kjca UserfilterIntegrationTest'

if [ "$#" -lt 2 ] ; then
  echo "$0: <tenant> <test> [remote-port]"

  exit 1
fi

if [ ! -z "$3" ] ; then
    EXTRAARGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$3"
else
    EXTRAARGS=""
fi

set -o nounset
set -o errexit

readonly DIR=$(dirname $0)

source "${DIR}/_cassandra_docker.sh"

if [ ! -f comaas.jks ] ; then
    keytool -genkey -alias comaas -keyalg RSA -keystore comaas.jks -keysize 2048 \
      -dname "CN=com, OU=COMaaS, O=eBay Classifieds, L=Amsterdam, S=Noord-Holland, C=NL" \
      -storepass 'comaas' -keypass 'comaas'

    # Install eBay SSL CA v2

    curl -sO "http://pki.corp.ebay.com/root-certs-pem.zip" && unzip root-certs-pem.zip

    for f in root-certs-pem/*.pem; do
        keytool -importcert -keystore comaas.jks -storepass 'comaas' -file $f -alias $f -noprompt
    done

    rm -rf root-certs-pem.zip root-certs-pem
fi

function log() {
    echo "[$(date)]: $*"
}

function fatal() {
    log $*
    exit 1
}

startCassandra
trap "stopCassandra" EXIT

MAVEN_OPTS="$EXTRAARGS" mvn -DtestLocalCassandraPort=${CASSANDRA_CONTAINER_PORT} -Drevision=123 -Djavax.net.ssl.trustStore=comaas.jks -Djavax.net.ssl.trustStorePassword=comaas  -Dlogback.debug=true -U -s etc/settings.xml -T0.5C -am -DfailIfNoTests=false -P "$1,"'core,core-tests,!distribution' clean package -Dtest=$2

stopCassandra
