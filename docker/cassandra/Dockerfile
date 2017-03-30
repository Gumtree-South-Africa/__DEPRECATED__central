FROM cassandra:2.1.15

RUN mkdir -p /tmp/var/lib/cassandra /etc/cassandra \
    && chown -R cassandra:cassandra /tmp/var/lib/cassandra /etc/cassandra \
    && chmod 777 /tmp/var/lib/cassandra /etc/cassandra

RUN sed -i "s~/var/lib/cassandra~/tmp/var/lib/cassandra~g" /etc/cassandra/cassandra.yaml

COPY *.cql /tmp/
RUN cassandra & \
    sleep 20 \
    && cqlsh localhost 9042 -e "CREATE KEYSPACE replyts2 WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1} AND durable_writes = true;" \
    && cqlsh localhost 9042 -k "replyts2" -f /tmp/cassandra_schema.cql \
    && cqlsh localhost 9042 -k "replyts2" -f /tmp/cassandra_volume_filter_schema.cql \
    && cqlsh localhost 9042 -k "replyts2" -f /tmp/cassandra_messagebox_schema.cql \
    && cqlsh localhost 9042 -k "replyts2" -f /tmp/cassandra_messagecenter_schema.cql

RUN chmod -R 777 /tmp/var/lib/cassandra /etc/cassandra
