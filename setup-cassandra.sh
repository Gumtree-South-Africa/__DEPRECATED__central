#!/bin/sh
#
# If necessary, download Cassandra from http://archive.apache.org/dist/cassandra/2.1.11/

/opt/apache-cassandra-2.1.11/bin/cqlsh replyts.dev.kjdev.ca 9042 -f core-runtime/src/main/resources/cassandra_schema.cql
/opt/apache-cassandra-2.1.11/bin/cqlsh replyts.dev.kjdev.ca 9042 -k replyts2 -f replyts2-mp-plugins/replyts2-filter-volume/src/main/resources/cassandra_volume_filter_schema.cql
/opt/apache-cassandra-2.1.11/bin/cqlsh replyts.dev.kjdev.ca 9042 -k replyts2 -f cassandra_messagebox_schema.cql
