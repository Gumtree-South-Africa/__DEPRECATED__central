# Cassandra data model

Here are the operations that we need to support:

## Store new message

There are 2 cases:

-A- New conversation

    check if conversation is blocked by checking if there is a row in blocked_users between the two parties

    new row in mb_unread_counters_by_ad
 
    new row in mb_conversations_index
    new row in mb_conversations
    new row in mb_messages

-B- Existing conversation

    check if conversation is blocked by checking if there is a row in blocked_users between the two parties
    check visibility 
        if displayed continue
        if archived continue, but update visibility to displayed (basically un-archive conversation - same as Facebook)
        if deleted stop (don't store the message anymore)

    update existing row in mb_unread_counters_by_ad
    
    update existing row in mb_conversations_index
    update existing row in mb_conversations with latest message and possibly visibility (see above)
    new row in mb_messages

## Get unread counts

    read all row in mb_unread_counters_by_ad for postbox and possibly ad id

## Mark a single conversation as read

    update existing row in mb_unread_counters_by_ad
    fetch updated conversation from mb_conversations + mb_messages + blocked_users if include _body parameter is specified

## Fetch full postbox: a list of conversations

-A- ordered by last message received timestamp.

    PARALLEL read all postbox rows in mb_unread_counters_by_ad
    PARALLEL read all postbox rows in mb_conversations_index
                read relevant (paginated) rows in mb_conversations
                read relevant rows from blocked_users

-B- grouped by ad and ordered by last message received timestamp.

    similar to above

## Fetch conversation

    PARALLEL read 1 conversation row in mb_unread_counters_by_ad
    PARALLEL read 1 row in mb_conversations
             read relevant row from blocked_users
    PARALLEL read relevant (paginated) rows in mb_messages

## Delete conversation

    reset to 0 for conversation id in mb_unread_counters_by_ad
    update visibility to deleted in mb_conversations_index
    update visibility to deleted in mb_conversations

## Cleanup old conversations

    stream through table mb_conversations_index
    then for all old conversations do:
        delete 1 row in mb_conversations_index
        delete 1 row in mb_conversations
        delete 1 row in mb_unread_counters_by_ad
        delete multiple rows in mb_messages (by conversation_id)
   
## Cleanup unblocked users rows                

    stream through table blocked_users or delete rows based on state in the primary key or just delete on unblock operation

## (Future) Robot messages

--> already supported (use well known sender_user_id)

## (Future) Attachments

* prepared field `attachment_ids`
* see cql file for proposed table `mb_attachments`

## Fetch conversations for an ad (for VIP and RYI)

    PARALLEL get ad related rows rom mb_unread_counters_by_ad
    PARALLEL get ad related rows rom mb_conversations_index
             read relevant (paginated) rows from mb_conversations
             read relevant rows from blocked_users

# Migration

* Remove table `mb_postbox`, move data to other tables.
* While migration job is back filling history, do _not_ write to `mb_unread_counters`.
* While migration job is back filling history, _only_ write to `mb_conversations_by_last_message` when
  existing `last_message_id` is older then `message_id` from back fill message.
* `mb_conversations`, `mb_messages` and `mb_attachments` are constructed such that they can be rewritten all the
  time and produce the same results (assumption: we can reuse the `message_id` from core).

# Changes for the new model to support response speed and rate:
* Store initial conversation type (bid or asq, etc)
* Store conversation initiator (buyer or seller)

----

# Random notes (to be cleaned up)


Too many dates:

- We are not using modifiedAt/createdAt, only lastMessageAddedAt.
- CreatedAt is used for cleanup in EbayK code.
- lastMessageAddedAt is used throughout

We can move changing data from conversationThread to separate table:
  * modifiedAt  <- not sure, its nice for ETAG support
  * numUnreadMessages <- already there
  * receivedAt <- unsure what it means, let remove it
  * previewLastMessage
  * lastMessageCreatedAt <- replaces receivedAt


Rename unread table to postbox, add all data that changes for each mail (except maybe mail text preview).

Rename postbox table to conversation table, add data that is stable for a conversation.

Idea: load all rows for a postbox, then sort, then paginate, then fetch stable data for conversation.


More ideas: move rendered message text to 3rd table.

Investigate what compaction is the best. Leveled compaction for tables with many mutations?
<-- will fail on really large inboxes, each write triggers a compaction

"Dropped gc_grace_period and increased repair frequency in favor of smalle but frequent ones."
<-- drops tumbstones quicker (otherwise they are kept even though a compaction is started)

Use circuit breaker to talk with Cassandra.
Manage resources (connections).


http://www.youneeq.ca/cassandra/


Yammer:
Unread messages duplicated in another table for faster counting.


probalistic trimming: trim only when you think it is a good moment to do so. For example,
delay trimming a normal inbox after x entries, but trim a very rapidly growing inbox after 10 x entries.



idea: trim old data on read

-- ##### TODOs #####

-- create new cleanup job
-- set a smaller time limit for tombstone retention (currently set at default level of 10 days - AND gc_grace_seconds = 864000)
-- use leveled compaction for mutating tables (mb_ad_conversations_idx, mb_ad_conversations etc.) - since it
-- will limit the number of SSLTables and improve read latency

-- ##### ??? #####

-- have separate tables or partitions based on visibility of conversations and conversation types
-- (currently just ad conversations, but we can also have buyer to seller direct conversation or group conversations)

-- ##### Limitations #####

-- can't have IN query when a collection column is being selected
-- https://inoio.de/blog/2016/01/13/cassandra-to-batch-or-not-to-batch/
-- use ListenableFuture or CompletableFuture and see if it's ok to not use logged batches to improve performance

-- Cassandra tables to be used in a future iteration (required for system messages)

CREATE TABLE IF NOT EXISTS mb_ad_conversation_participants (
    adid text,
    convid text,
    participantids text,
    PRIMARY KEY (adid, convid)
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Stores ad conversation participants. Columns: ad id, conversation id, participant ids'
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

CREATE TABLE IF NOT EXISTS mb_conversation_participants (
    participantid text,
    convid text,
    participantids text,
    PRIMARY KEY ((participantid, convid))
) WITH bloom_filter_fp_chance = 0.01
    AND caching = '{"keys":"ALL", "rows_per_partition":"NONE"}'
    AND comment = 'Stores ad conversation participants. Columns: ad id, conversation id, participant ids'
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