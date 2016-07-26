#!/usr/bin/env bash
#
# jenkins-deploy-comaasqa.sh deploys a comaas package to the comaasqa env

set -o nounset
# Not exiting on error, since we have a retry mechanism in here
# set -o errexit

ATTEMPTS=20

COUNT=3
STAGGER=30000000000

function usage() {
  cat <<- EOF
  Usage: jenkins-deploy-comaasqa.sh <tenant> <build_dir> <artifact_name> <git_hash>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  TENANT=$1
  BUILD_DIR=$2
  ARTIFACT_NAME=$3
  GIT_HASH=$4
}

function deploy() {
  # Send the job to Nomad
  TIMESTAMP=(${BUILD_DIR:(-13)})
  MD5=($(md5sum -b ${BUILD_DIR}/${ARTIFACT_NAME}))
  PORT=$(tar -xzf ${BUILD_DIR}/${ARTIFACT_NAME} --to-command="grep ^replyts.http.port=" ./conf/replyts.properties | cut -d'=' -f2)

  cp distribution/nomad/comaas_deploy_jenkins.json comaas_deploy_jenkins.json

  sed -i "s/COUNT/$COUNT/g" comaas_deploy_jenkins.json
  sed -i "s/STAGGER/$STAGGER/g" comaas_deploy_jenkins.json
  sed -i "s/TENANT/$TENANT/g" comaas_deploy_jenkins.json
  sed -i "s/GIT_HASH/$GIT_HASH/g" comaas_deploy_jenkins.json
  sed -i "s/TIMESTAMP/$TIMESTAMP/g" comaas_deploy_jenkins.json
  sed -i "s/PORT/$PORT/g" comaas_deploy_jenkins.json
  sed -i "s/md5/md5:$MD5/" comaas_deploy_jenkins.json
  sed -i "s~ARTIFACT~$ARTIFACT_NAME~" comaas_deploy_jenkins.json # Use ~ separator here since $ARTIFACT might contain slashes

  STAGGER_S=$((($STAGGER / 1000000000)))

  # Extract the Evaluation ID on return or fail if there isn't one
  EVALUATIONID=$(curl -s -X POST -d @comaas_deploy_jenkins.json http://consul001:4646/v1/jobs --header "Content-Type:application/json" | jq -r '.EvalID')

  if [ ! $? -eq 0 ] || [ -z "$EVALUATIONID" ] ; then
    echo "No evaluation ID returned - Nomad job failed"
    exit 1
  fi

  # Check this evaluation and any subsequent (NextEval) ones
  HEALTHY_HOSTS=0
  while [ 1 ] ; do
    echo "Checking eval $EVALUATIONID"

    # Try ATTEMPTS times to detect 'running' state on all nodes before failing
    for i in $(seq 1 $ATTEMPTS) ; do
      echo Sleeping for ${i}s
      sleep ${i}

      evalstate=$(curl -s http://consul001:4646/v1/evaluation/${EVALUATIONID}/allocations | \
      jq -c ".[] | { state: (.TaskStates[\"comaas-${TENANT}\"].State), id: .ID, running: (.TaskStates[\"comaas-${TENANT}\"].State == \"running\") }")

      if [ ! -z "$(echo ${evalstate} | grep "dead")" ]; then
        echo "At least one of the nodes reported \"dead\" state, exiting."
        echo ${evalstate}
        exit 1
      fi

      # All allocations on TENANT Nomad nodes should now be in the 'running' state (if not, fail)
      if [ ! -z "$(echo ${evalstate} | grep ':false')" ] ; then
        if [ $i -eq $ATTEMPTS ] ; then
          echo "Unable to get into the groove yo - waited a while but still non-running clients"
          echo ${evalstate}
          exit 1
        fi
      else
        break;
      fi
    done

    echo "All allocations are running"

    HOSTS=$(curl -s http://consul001:4646/v1/evaluation/${EVALUATIONID}/allocations | jq -r '.[].NodeID')
    echo "Checking hosts from eval $EVALUATIONID: $HOSTS"

    for HOSTID in ${HOSTS} ; do
      # Check the actual /health endpoint and compare the versions
      for i in $(seq 1 $ATTEMPTS); do
        sleep $i
        HOST=$(curl -s http://consul001:4646/v1/node/${HOSTID} | jq -r .Name)
        echo "Checking host: $HOST"

        if [ -n "$HOST" ]; then
          HEALTH=$(curl -s http://${HOST}:${PORT}/health)
          echo "${HOST}'s health is $HEALTH"
          if [ -n "$HEALTH" ]; then
            break
          fi
        fi

        if [ $i -eq $ATTEMPTS ]; then
          echo "Unable to get health from http://${HOST}:${PORT}/health, exiting"
          exit 1
        fi
      done

      ACTUAL=$(echo ${HEALTH} | jq -r ".version")
      if [ "$ACTUAL" = "$GIT_HASH" ] ; then
        echo "Host ${HOST} is running the correct version: $GIT_HASH"
        let HEALTHY_HOSTS+=1
      else
        echo "Host ${HOST} is NOT running the correct version: $ACTUAL (should be running $GIT_HASH)"
        exit 1
      fi
    done

    # Extract the NextEval - stop if this is the last one)
    EVALUATIONID="$(curl -s http://consul001:4646/v1/evaluation/${EVALUATIONID} | jq -r '.NextEval')"

    [[ -z "$EVALUATIONID" ]] && break

    # Sleep the number of seconds between staggered updates (rolling-update)
    echo "Waiting $STAGGER_S seconds until next rolling update will have been triggered by Nomad"
    sleep $STAGGER_S
  done

  if [ ! $COUNT -eq $HEALTHY_HOSTS ] ; then
    echo "Couldn't find the prerequisite number of healthy hosts ($COUNT vs. $HEALTHY_HOSTS)"
    exit 1
  fi

  echo "Successfully deployed $ARTIFACT_NAME to $COUNT comaasqa nodes"
}

parseArgs $@
deploy
