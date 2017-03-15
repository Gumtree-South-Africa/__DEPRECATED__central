#! /bin/sh

# This script expects the json of an entire conversation in a file and outputs:
#    .../{input filename/000-ConversationCreatedEvent.json
#    .../{input filename/001-MessageAddedEvent.json
#    etc
#
# Now you can inspect the individual events or just do a ls -ls for their sizes

set -e -o pipefail

readonly file=${1}
readonly nr_events=$(($(jq '. | length' ${file})-1))
readonly target=${file}_events

mkdir -p ${target}

for i in $(seq -w 0 $nr_events); do
  echo "$i / $nr_events"
  type=$(jq ".[${i}].type" ${file} | cut -d\" -f2)
  jq ".[${i}]" ${file} > ${target}/${i}-${type}.json
done
