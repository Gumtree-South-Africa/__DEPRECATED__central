#!/bin/sh
set -e

BASE_DIR=/opt/replyts

CLASSPATH="/app/*"
REPO=${BASE_DIR}/lib
CONF_DIR=${BASE_DIR}/conf

if [ -z ${TENANT} ] || [ -z ${NOMAD_PORT_http} ] || [ -z ${NOMAD_IP_hazelcast} ] || [ -z ${NOMAD_PORT_hazelcast} ] || [ -z ${NOMAD_PORT_prometheus} ] || [ -z ${NOMAD_REGION} ] || [ -z ${HEAP_SIZE} ]; then
    echo "Please set TENANT, NOMAD_PORT_http, NOMAD_IP_hazelcast, NOMAD_PORT_hazelcast, NOMAD_PORT_metrics, NOMAD_REGION, and HEAP_SIZE"
    exit 1
fi

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
export COMAAS_PROMETHEUS_PORT=${NOMAD_PORT_prometheus}

export region=${NOMAD_REGION}
export http_proxy=http://proxy.${region}.cloud.ecg.so:3128
export https_proxy=http://proxy.${region}.cloud.ecg.so:3128

/usr/bin/java \
    -Djava.security.egd=file:/dev/urandom \
    -DlogDir=/tmp \
    -Dtenant=${TENANT} \
    -Dservice.discovery.port=8500 \
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
    com.ecg.replyts.core.Application & pid=$!

wait
pid=
