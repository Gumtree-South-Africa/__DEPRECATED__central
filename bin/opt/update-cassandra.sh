#!/usr/bin/env bash

# Quick and dirty way to run arbitrary Cassandra commands on a bunch of tenant C* keyspaces

read -r -d '' QUERY << EOF
DROP TABLE core_held_mail;
CREATE TABLE core_held_mail (
    message_id text PRIMARY KEY,
    mail_data blob,
    mail_date timestamp
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';
EOF

ESCAPED_QUERY=$(echo $QUERY | tr $'\n' ' ' | sed 's/\ $//' | sed 's/"/\\"/g')

# All tenants = (ca au ek mo mp uk it bt) but only (mo mp) are on Cassandra!

for TENANT in mo mp ; do
	if [ "$TENANT" = "mp" ] ; then
		PREFIX="cass001"
		KEYSPACE="replyts2"
	else
		PREFIX="${TENANT}-cass001"
		KEYSPACE="${TENANT}_comaas"
	fi

	for ENV in qa ; do
		HOST="$PREFIX.comaas-$ENV"
		COMMAND=`echo 'cqlsh -k "'"$KEYSPACE"'" $(ifconfig eth0 | grep "inet\ " | cut -d: -f2 | cut -d" " -f1) -e "'"$ESCAPED_QUERY"'"'`

		ssh $HOST "$COMMAND"
	done
done
