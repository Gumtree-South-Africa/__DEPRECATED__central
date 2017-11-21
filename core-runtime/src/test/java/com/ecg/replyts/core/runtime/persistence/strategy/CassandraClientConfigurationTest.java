package com.ecg.replyts.core.runtime.persistence.strategy;

import com.ecg.replyts.core.runtime.persistence.strategy.CassandraPersistenceConfiguration.CassandraClientConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CassandraClientConfigurationTest {

    @Test
    public void getHostMetricName() {
        CassandraClientConfiguration configuration = new CassandraClientConfiguration();

        assertEquals("ek-cass001", configuration.getHostMetricName("ek-cass001.comaas-prod.dus1"));
        assertEquals("ek-cass002", configuration.getHostMetricName("ek-cass002.comaas-prod.dus1"));
        assertEquals("ek-cass003", configuration.getHostMetricName("ek-cass003.comaas-prod.dus1"));
        assertEquals("ek-cass004", configuration.getHostMetricName("ek-cass004.comaas-prod.dus1"));
    }
}