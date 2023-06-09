package com.ecg.replyts.core.webapi.control;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/health")
public class HealthController {
    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);

    private String hostname;

    private String searchClusterVersion;

    @Value("${VERSION:UNKNOWN}")
    private String version;

    @Value("${active.dc:unknown}")
    private String activeDc;

    @Value("${spring.application.name:unknown}")
    private String instanceName;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    @Value("#{'${persistence.cassandra.core.endpoint}'.split(',')}")
    private List<String> cassandraHosts;

    @Value("${search.es.endpoint:http://localhost:9200}")
    private String elasticSearchHost;

    @Value("${persistence.cassandra.core.keyspace:replyts2}")
    private String cassandraKeyspace;

    @Value("${search.es.clustername:elasticsearch}")
    private String searchClusterName;

    @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
    private String cassandraDc;

    @PostConstruct
    public void init() throws InterruptedException, UnknownHostException {
        this.searchClusterVersion = getSearchClusterVersion();
        this.hostname = getHostname();
    }

    @Autowired(required = false)
    private Client searchClient = null;

    @RequestMapping(method = RequestMethod.GET)
    public Health get() {
        return new Health();
    }

    @JsonPropertyOrder(value = { "version", "tenant", "instanceName", "hostname"}, alphabetic = true)
    class Health {
        private Health() {
        }

        public String getActiveDc() {
            return activeDc;
        }

        public String getVersion() {
            return version;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public String getTenant() {
            return tenant;
        }

        public String getElasticSearchHosts() {
            return elasticSearchHost;
        }

        public List<String> getCassandraHosts() {
            return cassandraHosts;
        }

        public String getCassandraKeyspace() {
            return cassandraKeyspace;
        }

        public String getCassandraDc() {
            return cassandraDc;
        }

        public String getSearchClusterName() {
            return searchClusterName;
        }

        public String getSearchClusterVersion() {
            return searchClusterVersion;
        }

        public String getHostname() {
            return hostname;
        }
    }

    private String getSearchClusterVersion() throws InterruptedException {
        if (searchClient == null) {
            return "unknown";
        }

        try {
            Set<String> versions = new HashSet<>();

            // If versions differ between cluster nodes, return a comma separated list instead
            searchClient.admin().cluster().prepareNodesInfo().execute().get().getNodes().forEach(info -> versions.add(info.getVersion().toString()));

            return StringUtils.collectionToDelimitedString(versions, ", ");
        } catch (ExecutionException | ElasticsearchException e) {
            LOG.error("Could not get ES version", e);
            return "error";
        }
    }

    private String getHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getCanonicalHostName();
    }
}
