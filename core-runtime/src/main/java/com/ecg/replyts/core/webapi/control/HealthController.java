package com.ecg.replyts.core.webapi.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/health")
public class HealthController {
    @Autowired
    private final DiscoveryClient discoveryClient = null;

    @RequestMapping(method = RequestMethod.GET)
    public Health get() throws Exception {
        String version = getClass().getPackage().getImplementationVersion();

        return new Health(version, getCassandraURIs());
    }

    public List<String> getCassandraURIs() {
        List<ServiceInstance> list = discoveryClient.getInstances("cassandra");

        return list.stream()
                .map(instance -> instance.getUri().toString())
                .collect(Collectors.toList());
    }

    class Health {
        String version;

        List<String> cassandraURIs;

        private Health(String version, List<String> cassandraURIs) {
            this.version = version;
            this.cassandraURIs = cassandraURIs;
        }

        public String getVersion() {
            return version;
        }

        public List<String> getCassandraURIs() {
            return cassandraURIs;
        }

        public String getInstanceId() {
            return discoveryClient.getLocalServiceInstance().getServiceId();
        }
    }
}
