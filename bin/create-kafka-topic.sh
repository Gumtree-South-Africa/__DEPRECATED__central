#!/usr/bin/env bash

function usage() {
  cat <<- EOF
  Usage: create-kafka-topic.sh <topic_name> <replication_factor> <kafka_partition_num>
EOF
  exit
}

function parseArgs() {
  # check amount of args
  [[ $# == 0 ]] && usage

  readonly TOPIC_NAME=$1
  readonly REPLICATION_FACTOR=$2
  readonly KAFKA_PARTITION_NUM=$3

}


function createTopic() {
	/opt/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor $REPLICATION_FACTOR --partitions $KAFKA_PARTITION_NUM --topic "$TOPIC_NAME"
}

parseArgs $@
createTopic
