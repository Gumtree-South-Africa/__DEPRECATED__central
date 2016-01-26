#!/bin/sh
#
# If necessary, download cassandra from http://archive.apache.org/dist/cassandra/2.1.11/

/opt/apache-cassandra-2.1.11/bin/cqlsh replyts.dev.kjdev.ca 9042 -f core-runtime/src/main/resources/cassandra_schema.cql