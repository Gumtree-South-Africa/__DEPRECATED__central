package com.ecg.messagecenter.pushmessage.send.discovery;

import ca.kijiji.discovery.ServiceDirectory;
import ca.kijiji.discovery.consul.DnsConsulCatalog;
import com.codahale.metrics.MetricRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;

@Configuration
public class ServiceDiscoveryConfig {

    @Value("${consul.host:localhost}")
    String consulHost;

    @Value("${consul.port:8600}")
    Integer consulPort;

    MetricRegistry metricRegistry;

    @Autowired
    protected ServiceDiscoveryConfig(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Bean
    public ServiceEndpointProvider serviceEndpointProvider() throws UnknownHostException {
        return new ServiceEndpointProvider(serviceDirectory(), new ServiceLookupMetrics(metricRegistry));
    }

    private ServiceDirectory serviceDirectory() throws UnknownHostException {
        return DnsConsulCatalog.usingUdp(consulHost, consulPort);
    }

}


