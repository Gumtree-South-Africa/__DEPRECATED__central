#!/bin/sh
#
# If necessary, download Cassandra from http://archive.apache.org/dist/cassandra/2.1.13/

HOST=$1
KEYSPACE=$2

if [ -z "$HOST" ] ; then
    HOST="replyts.dev.kjdev.ca"
fi

if [ -z "$KEYSPACE" ] ; then
    KEYSPACE="replyts2"
fi

if [ -z "$CQLSH_PATH" ] ; then
    shopt -s nullglob

    FILES=(/opt/apache-cassandra* /opt/cassandra*)
 
    if [ -d "${FILES[0]}/bin" ] ; then
      CQLSH_PATH="${FILES[0]}/bin"
    else
       : ${CQLSH_PATH:?"Please set the path to cqlsh e.g. /opt/apache-cassandra-2.1.13/bin"}
    fi
 else
    : ${CQLSH_PATH:?"Please set the path to cqlsh e.g. /opt/apache-cassandra-2.1.13/bin"}
 fi
  
echo "Using CQLSH_PATH = $CQLSH_PATH"

# Don't fail with an error - this is typically desirable behavior
$CQLSH_PATH/cqlsh $HOST 9042 -e "USE system; SELECT keyspace_name FROM schema_keyspaces WHERE keyspace_name = '$KEYSPACE';" | grep '0 rows' >/dev/null ||
  echo "Keyspace $KEYSPACE already exists - not recreating it" && exit 0

# $CQLSH_PATH/cqlsh $HOST 9042 -e "DROP KEYSPACE IF EXISTS $KEYSPACE;"

$CQLSH_PATH/cqlsh $HOST 9042 -e "CREATE KEYSPACE $KEYSPACE WITH replication = {'class': 'NetworkTopologyStrategy', 'ams1': '2'} AND durable_writes = true;"
$CQLSH_PATH/cqlsh $HOST 9042 -k "$KEYSPACE" -f core-runtime/src/main/resources/cassandra_schema.cql
$CQLSH_PATH/cqlsh $HOST 9042 -k "$KEYSPACE" -f filters/mp-filter-volume/src/main/resources/cassandra_volume_filter_schema.cql
$CQLSH_PATH/cqlsh $HOST 9042 -k "$KEYSPACE" -f etc/cassandra_messagebox_schema.cql

if [ $? -eq 0 ] ; then
    echo "Keyspace $KEYSPACE was successfully created"
else
    echo "Keyspace $KEYSPACE could not be created"
    exit 1
fi
