package com.ecg.replyts.core.runtime.cluster;

/**
 * Current Cluster Operation mode. Important for dual datacenter operations, where one datacenter loses connections to the other one.
 * these modes help recovering in such a situation. please refer to the <a href="https://github.scm.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two%20Datacenter%20Operations">article on dual datacenters in the wiki</a>.
 */
public enum ClusterMode {
    /**
     * Everything is okay, the ReplyTS cluster is fully healthy and operates in normal mode
     */
    OK,
    /**
     * ReplyTS has lost connection to one datacenter. No new mails are processed. Awaiting human interaction to continue
     */
    BLOCKED,
    /**
     * Run in Failover mode (one datacenter only - needs to be explicitely enabled). once datacenters reconnect, ReplyTS will enter blocked mode again.
     */
    FAILOVER
}
