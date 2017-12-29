#!/bin/sh

#$NOMAD_RERGION is made available through nomad and is usable as an environment variable. This will contain the lowercase ams1 or dus1 region

ACTIVEDC=`dig +short ek.lp.comaas.cloud TXT | sed 's/^....//' | sed 's/.$//' | awk '{print tolower($0)}' `

if [ "$ACTIVEDC" != "ams1" ] && [ "$ACTIVEDC" != "dus1" ]
then
  echo "I don't know the returned dc, please check if lookup fails"
  exit 1
fi

if [ "$NOMAD_REGION" == "$ACTIVEDC" ]
then
  echo "This is the activedc, not running the cronjob"
  exit 0
else
  echo "This is not the active dc, so running the command"
  java -cp /:/cronjobs-cleanup-conversations-1-SNAPSHOT-jar-with-dependencies.jar -jar cronjobs-cleanup-conversations-1-SNAPSHOT.jar
fi
