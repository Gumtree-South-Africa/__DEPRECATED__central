CREATE TABLE IF NOT EXISTS mb_conversation_unread_counts (
    pbid text,
    convid text,
    unread int,
    PRIMARY KEY (pbid, convid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Contains conversation unread messages counts. Columns: postbox id, conversation id, number of unread conversation messages'
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

CREATE TABLE IF NOT EXISTS mb_ad_conversation_unread_counts (
    pbid text,
    adid text,
    convid text,
    unread int,
    PRIMARY KEY (pbid, adid, convid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Contains conversation unread messages counts clustered by ad id. Columns: postbox id, ad id, conversation id, number of unread conversation messages'
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'}
    AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND dclocal_read_repair_chance = 0.1
    AND default_time_to_live = 0
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair_chance = 0.0
    AND speculative_retry = '99.0PERCENTILE';

CREATE TABLE IF NOT EXISTS mb_ad_conversation_idx (
    pbid text,
    adid text,
    convid text,
    visibility int,
    latestmsgid timeuuid,
    PRIMARY KEY (pbid, adid, convid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Used for paginating conversations. Columns: postbox id, ad id, conversation id, visibility (1 - Active, 2 - Archived), latest message id'
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

CREATE TYPE IF NOT EXISTS conversation_participant (
    participantid text,
    name text,
    email text,
    role text
);

CREATE TYPE IF NOT EXISTS conversation_message (
    msgid timeuuid,
    msg text,
    senderid text,
    type text
);

CREATE TABLE IF NOT EXISTS mb_conversations (
    pbid text,
    convid text,
    adid text,
    visibility int,
    notifynew int,
    meparticipant frozen<conversation_participant> STATIC,
    otherparticipant frozen<conversation_participant>,
    latestmsgprev frozen<conversation_message>,
    PRIMARY KEY (pbid, convid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Stores ad conversations. Columns: postbox id, conversation id, ad id, visibility (1 - Active, 2 - Archived), notify on new messages (1 - Notify, 2 - Mute), me participant, other participant, latest message preview'
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

CREATE TABLE IF NOT EXISTS mb_blocked_users (
    blockerid text,
    blockeeid text,
    blockdate timestamp,
    PRIMARY KEY (blockerid, blockeeid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Used to block users. Columns: blocker user id, blockee user id, blocking date'
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

CREATE TABLE IF NOT EXISTS mb_messages (
    pbid text,
    convid text,
    msgid timeuuid,
    msg text,
    senderid text,
    type text,
    PRIMARY KEY ((pbid, convid), msgid)
) WITH CLUSTERING ORDER BY (msgid DESC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Stores conversation messages. Supports cursor-based pagination. Columns: postbox id, conversation id, message id, message text, message type (asq, abq, bid, chat, email), sender user id, message type, attachments urls (they will be stored in an external object store)'
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

CREATE TABLE IF NOT EXISTS mb_ad_conversation_modification_idx (
    pbid text,
    convid text,
    msgid timeuuid,
    adid text,
    PRIMARY KEY ((pbid, convid), msgid)
) WITH CLUSTERING ORDER BY (msgid DESC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Used for cleanup of old conversations. Columns: postbox id, conversation id, message id, ad id'
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

CREATE TABLE IF NOT EXISTS mb_conversation_modification_idx_by_date (
    modifdate timestamp,
    msgid timeuuid,
    pbid text,
    convid text,
    PRIMARY KEY (modifdate, msgid, pbid, convid)
) WITH CLUSTERING ORDER BY (msgid DESC, pbid ASC, convid ASC)
    AND bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Used for cleanup of old conversations. Columns: year, month, day, message id, postbox id, conversation id'
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