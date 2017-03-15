#!/bin/sh

# Get the conversation with conversationId $1 from Riak and print the number of events

readonly riak_host=10.47.72.183
readonly prefix=coma

# echo "Checking conversationId $1 in mobile.de's riak"

curl -s "http://${riak_host}:8098/buckets/${prefix}conversation/keys/$1" | gunzip -c | jq '. | length'
