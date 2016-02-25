#!/bin/bash
#
# Stops Reply T&S
#
# Expected current working directory is $project/target/replyts/.
#

RTSDIR=$PWD
PIDFILE=${RTSDIR}/run.pid

if [[ ! -s ${PIDFILE} ]]; then
  echo "Can not find PID of Reply T&S, NOT stopped"
  exit 1
fi

PID=$(< $PIDFILE)

kill $PID

sleep 3

if ps -p ${PID} >/dev/null; then
  echo ""
  echo "Reply T&S is still running, doing hard kill"
  echo ""
  kill -9 $PID
  sleep 1

  if ps -p ${PID} >/dev/null; then
    echo ""
    echo "Reply T&S is still running, failing the build"
    echo ""
    exit 1
  fi
fi

rm $PIDFILE
echo "Reply T&S stopped"
