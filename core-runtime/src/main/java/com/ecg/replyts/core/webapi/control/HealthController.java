package com.ecg.replyts.core.webapi.control;

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
// RP: added /internal/healthcheck for GTUK legacy deploy. Can be removed after we move to the Cloud.
@RequestMapping(value = {"/health", "/internal/healthcheck"})
public class HealthController {
    private static final Logger LOG = LoggerFactory.getLogger(HealthController.class);

    private String version = getClass().getPackage().getImplementationVersion();

    private String hostname;

    private String searchClusterVersion;

    @Value("${spring.application.name:unknown}")
    private String instanceName;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    @Value("${spring.cloud.config.discovery.enabled:false}")
    private boolean isDiscoveryEnabled;

    @Value("${persistence.strategy:unknown}")
    private String conversationRepositorySource;

    @Value("#{'${persistence.riak.datacenter.primary.hosts:unknown}'.split(',')}")
    private List<String> riakHosts;

    @Value("#{'${persistence.cassandra.endpoint:unknown}'.split(',')}")
    private List<String> cassandraHosts;

    @Value("#{'${search.es.endpoints:unknown}'.split(',')}")
    private List<String> elasticSearchHosts;

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String riakBucketPrefix;

    @Value("${persistence.cassandra.keyspace:replyts2}")
    private String cassandraKeyspace;

    @Value("${search.es.clustername:unknown}")
    private String searchClusterName;

    @Value("${persistence.cassandra.dc:#{systemEnvironment['region']}}")
    private String cassandraDc;

    @PostConstruct
    public void init() throws InterruptedException, UnknownHostException {
        this.searchClusterVersion = getSearchClusterVersion();
        this.hostname = getHostname();
    }

    @Autowired(required = false)
    private Client searchClient = null;

    @RequestMapping(method = RequestMethod.GET)
    public Health get() throws Exception {
        return new Health();
    }

    class Health {
        private Health() {
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

        public Boolean isDiscoveryEnabled() {
            return isDiscoveryEnabled;
        }

        public String getConversationRepositorySource() {
            return conversationRepositorySource;
        }

        public List<String> getElasticSearchHosts() {
            return elasticSearchHosts;
        }

        public List<String> getRiakHosts() {
            return riakHosts;
        }

        public List<String> getCassandraHosts() {
            return cassandraHosts;
        }

        public String getRiakBucketPrefix() {
            return riakBucketPrefix;
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

        public String getSearchClusterVersion() throws InterruptedException {
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
            searchClient.admin().cluster().prepareNodesInfo().execute().get().forEach(info -> versions.add(info.getVersion().toString()));

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
