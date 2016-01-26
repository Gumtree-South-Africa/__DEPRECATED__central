#!/bin/bash
#
# Starts Reply T&S
#
# Expected current working directory is $project/target/replyts/.
#

RTSDIR=$PWD
PIDFILE=${RTSDIR}/run.pid

PID=$(ps -ef | grep "app.name=replyts" | grep -v grep | awk '{print $2}')
[[ -n "${PID}" ]] && if ps -p ${PID} >/dev/null; then
  echo ""
  echo "Reply T&S is still running, please make sure it is not started. Aborting..."
  echo ""
  exit 1
fi

CONFDIR="${RTSDIR}/app/conf"
LOGDIR="${RTSDIR}/log"
STDOUT="${LOGDIR}/stdout"

# Rename extracted distribution to 'app' (unless it was already done)
[[ -d app ]] || mv distribution-* app

# create directories and install config
cp -R ../test-classes/conf/* ${CONFDIR}

mkdir -p ${LOGDIR}
mkdir -p "${RTSDIR}/dropfolder"

export JAVA_OPTS="-Xmx512m -DconfDir=${CONFDIR} -DlogDir=${LOGDIR}"

chmod +x ./app/bin/replyts
./app/bin/replyts &>"${STDOUT}" &

echo "Waiting for Reply T&S start"
sleep 5
ps -ef | grep "app.name=replyts" | grep -v grep | awk '{print $2}' > ${PIDFILE}

echo "DONE starting Reply T&S, PID = "$(< ${PIDFILE})
