#!/bin/bash
#
# If necessary, download Cassandra from http://archive.apache.org/dist/cassandra/2.1.13/

# Usage: bin/setup-cassandra.sh <host> <keyspace> <isnondev>
#   host:       The host to connect to, default: replyts.dev.kjdev.ca
#   keyspace:   The keyspace to create, default: replyts2
#   isnondev:   Allowed values: <empty>, 1, or 'prod', default: <empty>.
#       empty:  create keyspace in dc 'datacenter1' with replication factor 1 (dev)
#       1:      create keyspace in dc 'ams1' with replication factor 3 (qa, sandbox)
#       prod:  create keyspace in dc 'ams1' and 'dus1' with replication factor 3 (prod)

HOST=$1
KEYSPACE=$2
ISNONDEV=$3

function fail_gracefully {
    echo $1
    exit 0
}

if [ -z "${HOST}" ] ; then
    HOST="replyts.dev.kjdev.ca"
fi

if [ -z "$KEYSPACE" ] ; then
    KEYSPACE="replyts2"
fi

if [ -z "$(which cqlsh)" ] ; then
    export PATH=$PATH:/opt/cassandra/bin:/usr/sbin
    if [ -z "$(which cqlsh)" ] ; then
        echo "Could not find cqlsh on PATH"
        exit 1
    fi
fi

echo "Creating keyspace $KEYSPACE on host $HOST"

cqlsh ${HOST} 9042 -e "USE system;" || { echo "Cannot connect to Cassandra on $HOST! Exiting."; exit 1; }

# Don't fail with an error - this is typically desirable behavior
cqlsh ${HOST} 9042 -e "USE system; SELECT keyspace_name FROM schema_keyspaces WHERE keyspace_name = '$KEYSPACE';" | grep '0 rows' >/dev/null ||
  fail_gracefully "Keyspace $KEYSPACE already exists - not recreating it"

# cqlsh ${HOST} 9042 -e "DROP KEYSPACE IF EXISTS $KEYSPACE;"

if [[ ! -z "$ISNONDEV" ]]; then
    if [[ "$ISNONDEV" -eq "prod" ]]; then
        echo "Creating keyspace with replication factor of 3 in 'ams1' and 'dus1'"
        cqlsh ${HOST} 9042 -e  "CREATE KEYSPACE $KEYSPACE WITH replication = {'class': 'NetworkTopologyStrategy', 'ams1': '3', 'dus1': '3'} AND durable_writes = true;"
    else
        echo "Creating keyspace with replication factor of 3 in 'ams1'"
        cqlsh ${HOST} 9042 -e  "CREATE KEYSPACE $KEYSPACE WITH replication = {'class': 'NetworkTopologyStrategy', 'ams1': '3'} AND durable_writes = true;"
    fi
else
    echo "Creating keyspace with replication factor of 1 in 'datacenter1'"
    cqlsh ${HOST} 9042 -e "CREATE KEYSPACE $KEYSPACE WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1} AND durable_writes = true;"
fi
cqlsh ${HOST} 9042 -k "$KEYSPACE" -f core-runtime/src/main/resources/cassandra_schema.cql
cqlsh ${HOST} 9042 -k "$KEYSPACE" -f filters/mp-filter-volume/src/main/resources/cassandra_volume_filter_schema.cql
cqlsh ${HOST} 9042 -k "$KEYSPACE" -f messagecenters/mp-messagecenter/src/main/resources/cassandra_new_messagebox_schema.cql
cqlsh ${HOST} 9042 -k "$KEYSPACE" -f messagecenters/core-messagecenter/src/main/resources/cassandra_core_messagecenter_schema.cql

if [ $? -eq 0 ] ; then
    echo "Keyspace $KEYSPACE was successfully created"
else
    echo "Keyspace $KEYSPACE could not be created"
    exit 1
fi
