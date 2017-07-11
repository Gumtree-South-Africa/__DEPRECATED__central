package com.ecg.replyts.core.runtime.cluster;

/**
 * MBean controlling cluster mode for dual datacenter split brain failover. See {@link ClusterMode} and
 * <a href="https://github.scm.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two%20Datacenter%20Operations">article on dual datacenters in the wiki</a>.
 *
 * @author mhuttar
 */
@Deprecated
public interface ClusterModeControlMBean {
    void switchToNormalMode();

    void switchToFailoverMode();
}
