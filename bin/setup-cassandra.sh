#!/bin/sh
#
# If necessary, download Cassandra from http://archive.apache.org/dist/cassandra/2.1.13/

if [ -z "$CQLSH_PATH" ] ; then
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

$CQLSH_PATH/cqlsh replyts.dev.kjdev.ca 9042 -f core-runtime/src/main/resources/cassandra_schema.cql
$CQLSH_PATH/cqlsh replyts.dev.kjdev.ca 9042 -k replyts2 -f filters/mp-filter-volume/src/main/resources/cassandra_volume_filter_schema.cql
$CQLSH_PATH/cqlsh replyts.dev.kjdev.ca 9042 -k replyts2 -f etc/cassandra_messagebox_schema.cql
