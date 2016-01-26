package com.ecg.replyts.core.runtime.persistence;

import com.ecg.replyts.core.runtime.cluster.RiakHostConfig;
import com.ecg.replyts.core.runtime.cluster.RiakHostConfig.Host;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mhuttar
 */
public class RiakHostConfigTest {

    @Test
    public void withTwoHosts() {
        RiakHostConfig config = new RiakHostConfig("host1,host2", 0, 0);

        assertThat(config.getHostList().size()).isEqualTo(2);

        Host host1 = config.getHostList().get(0);
        assertThat(host1.getHost()).isEqualTo("host1");

        Host host2 = config.getHostList().get(1);
        assertThat(host2.getHost()).isEqualTo("host2");
    }

    @Test
    public void withOneHost() {
        RiakHostConfig config = new RiakHostConfig("host1", 42, 44);

        Host host = config.getHostList().get(0);

        assertThat(config.getHostList().size()).isEqualTo(1);
        assertThat(host.getHost()).isEqualTo("host1");
    }

}
