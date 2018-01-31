#!/bin/sh
set -e

BASE_DIR=/opt/replyts

CLASSPATH="${BASE_DIR}/lib/*"
REPO=${BASE_DIR}/lib
CONF_DIR=${BASE_DIR}/conf

[ -z ${TENANT} ] && { echo "Please set TENANT"; exit 1; }
[ -z ${NOMAD_PORT_http} ] && { echo "Please set NOMAD_PORT_http"; exit 1; }
[ -z ${NOMAD_IP_hazelcast} ] && { echo "Please set NOMAD_IP_hazelcast"; exit 1; }
[ -z ${NOMAD_PORT_hazelcast} ] && { echo "Please set NOMAD_PORT_hazelcast"; exit 1; }
[ -z ${NOMAD_REGION} ] && { echo "Please set NOMAD_REGION"; exit 1; }
[ -z ${HEAP_SIZE} ] && { echo "Please set HEAP_SIZE"; exit 1; }

stop () {
    sleep 30 # Grace period for still running http connections
    while kill -0 ${pid} 2> /dev/null
    do
        kill -TERM ${pid}
        sleep 5
    done
    exit 0
}

trap stop SIGTERM

export COMAAS_HTTP_PORT=${NOMAD_PORT_http}
export COMAAS_HAZELCAST_IP=${NOMAD_IP_hazelcast}
export COMAAS_HAZELCAST_PORT=${NOMAD_PORT_hazelcast}
export region=${NOMAD_REGION}
export http_proxy=http://proxy.${region}.cloud.ecg.so:3128
export https_proxy=http://proxy.${region}.cloud.ecg.so:3128
export swift_authentication_url=https://keystone.${region}.cloud.ecg.so/v2.0

/usr/bin/java \
    -DlogDir=/tmp \
    -Dtenant=${TENANT} \
    -Dfile.encoding=UTF-8 \
    -XX:+PreserveFramePointer \
    -Xms${HEAP_SIZE} \
    -Xmx${HEAP_SIZE} \
    ${JAVA_OPTS} \
    -XX:-HeapDumpOnOutOfMemoryError \
    -Djava.awt.headless=true \
    -Dcom.datastax.driver.FORCE_NIO=true \
    -XX:+PrintGCDetails \
    -Xloggc:/alloc/logs/gc.log \
    -verbose:gc \
    -XX:+PrintGCTimeStamps \
    -XX:+PrintGCDateStamps \
    -XX:+PrintTenuringDistribution \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=7 \
    -XX:GCLogFileSize=128M \
    -XX:+PrintConcurrentLocks \
    -XX:+PrintClassHistogram \
    -XX:+PrintStringTableStatistics \
    -classpath "${CLASSPATH}" \
    -Dapp.name="comaas" \
    -Dapp.pid="$$" \
    -Dapp.home="${BASE_DIR}" \
    -Dbasedir="${BASE_DIR}" \
    -Dapp.repo="${REPO}" \
    com.ecg.replyts.core.Application & pid=$!

wait
pid=
