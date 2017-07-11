package com.ecg.replyts.core.api;

/**
 * Monitors the ReplyTS/Peristence Cluster and is aware of multiple datacenters. It can tell the application if all datacenters are available.
 */
public interface ClusterMonitor {
    /**
     * Checks if both datacenters can be contacted (and therefore are connected with each other). If there is only one datacenter
     * returns always true, because one datacenter is always connected to itself.
     */
    boolean allDatacentersAvailable();

    /**
     * string report on datacenter health for logs
     */
    String report();
}