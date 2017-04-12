FROM cassandra:2.1.15

RUN mkdir -p /tmp/var/lib/cassandra /etc/cassandra \
    && chown -R cassandra:cassandra /tmp/var/lib/cassandra /etc/cassandra \
    && chmod 777 /tmp/var/lib/cassandra /etc/cassandra

RUN sed -i "s~/var/lib/cassandra~/tmp/var/lib/cassandra~g" /etc/cassandra/cassandra.yaml

COPY *.cql /tmp/
RUN cassandra \
    && while [ $(cqlsh -e "USE system" || echo 1) ]; do sleep 0.5; done \
    && cqlsh -e "CREATE KEYSPACE replyts2 WITH replication = {'class': 'NetworkTopologyStrategy', 'datacenter1': 1} AND durable_writes = true;" \
    && echo "Created keyspace replyts2" \
    && cqlsh -k "replyts2" -f /tmp/cassandra_schema.cql \
    && echo "Created schema: core" \
    && cqlsh -k "replyts2" -f /tmp/cassandra_volume_filter_schema.cql \
    && echo "Created schema: volume filter" \
    && cqlsh -k "replyts2" -f /tmp/cassandra_messagebox_schema.cql \
    && echo "Created schema: messagebox" \
    && cqlsh -k "replyts2" -f /tmp/cassandra_messagecenter_schema.cql \
    && echo "Created schema: messagecenter" \
    && nodetool flush replyts2

HEALTHCHECK --interval=1s --timeout=1s --retries=1 CMD cqlsh -e "USE replyts2"

RUN chmod -R 777 /tmp/var/lib/cassandra /etc/cassandra
