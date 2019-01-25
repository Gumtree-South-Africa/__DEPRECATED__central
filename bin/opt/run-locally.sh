#!/usr/bin/env bash
set -e

CLASSPATH="distribution/target/dependency/*"

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  readonly TENANT=$1
  readonly DEBUG=$2

  if [[ $DEBUG == true ]]; then
    export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
  else
    export JAVA_OPTS=""
  fi
}

function usage() {
  cat <<- EOF
  Usage: run-locally.sh <tenant_name> <debug>
  where <debug> is optional, defaults to false
  Make sure to execute 'bin/build.sh -p' in central and 'make up' in comaas Docker project before invoking
  this script
EOF
  exit
}

# Load tenant's configuration
function loadConfig() {
# You might need to change the default network name comaasdocker_consul_1 for something else. Check your consul container network name with docker ps for this
docker run --network comaasdocker_default --rm --volume ${PWD}/distribution/conf/${TENANT}/docker.properties:/props.properties  \
  dock.es.ecg.tools/comaas/properties-to-consul:0.0.7 -consul http://consul:8500 -tenant ${TENANT}
}

stop () {
    while kill -0 ${pid}
    do
        kill -TERM ${pid}
        sleep 3
    done
    exit 0
}

function checkJavaVersion() {
    JAVA_VER=$(java -version 2>&1 | sed -n ';s/.* version "\(.*\)\.\(.*\)\..*"/\1\2/p;')
    if [ "$JAVA_VER" == 18 ]; then
        echo "ok, java is 1.8 "
    else
        echo "Please make sure you have Java version 8 on your classpath!" && exit 1
    fi
}

function runComaas() {
trap stop SIGINT SIGTERM EXIT SIGQUIT

export COMAAS_HTTP_PORT=8080
export COMAAS_HAZELCAST_IP=127.0.0.1
export COMAAS_HAZELCAST_PORT=9019
export COMAAS_PROMETHEUS_PORT=9020
export HEAP_SIZE=1G
export DISCOVERY_PORT=8599


/usr/bin/java \
    -Djava.security.egd=file:/dev/urandom \
    -DlogDir=/tmp \
    -Dtenant=${TENANT} \
    -Dreplyts.tenant=${TENANT} \
    -Dservice.discovery.port=${DISCOVERY_PORT} \
    -Dfile.encoding=UTF-8 \
    -XX:+PreserveFramePointer \
    -Xms${HEAP_SIZE} \
    -Xmx${HEAP_SIZE} \
    ${JAVA_OPTS} \
    -XX:-HeapDumpOnOutOfMemoryError \
    -Djava.awt.headless=true \
    -Dcom.datastax.driver.FORCE_NIO=true \
    -Dmail.mime.parameters.strict=false \
    -Dmail.mime.address.strict=false \
    -Dmail.mime.ignoreunknownencoding=true \
    -Dmail.mime.uudecode.ignoreerrors=true \
    -Dmail.mime.uudecode.ignoremissingbeginend=true \
    -Dmail.mime.multipart.allowempty=true \
    -classpath "${CLASSPATH}" \
    com.ecg.replyts.core.Application & pid=$!

wait
pid=
}

parseArgs $@
checkJavaVersion
loadConfig
runComaas
