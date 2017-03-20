# core-messagecenter

Originally taken from https://github.corp.ebay.com/ReplyTS/replyts2-message-box
(original git hash: ca4ebc3688d6797545cb3833c5976ffd73dfb47a)

# Description

Contains common code for both the old as well as the new messagecenter implementations.

## com.ecg.messagebox

Cassandra-only implementation originally developed by Marktplaats and currently used by Marktplaats and Mobile.de. The goal is to ultimately consolidate all COMaaS tenants onto this messagecenter implementation.

## com.ecg.messagecenter

Legacy implementation originally developed by eBay Kleinanzeigen on Riak. This implementation has since been refactored to also offer persistence via Cassandra. Cassandra persistence has been further refactored to resolve several performance bottlenecks inherent in the Riak-based design.
