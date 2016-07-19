package com.ecg.replyts.core.webapi.control;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/health")
public class HealthController {
    private String version = getClass().getPackage().getImplementationVersion();

    @Value("${spring.application.name:unknown}")
    private String instanceName;

    @Value("${replyts.tenant:unknown}")
    private String tenant;

    @Value("${spring.cloud.config.discovery.enabled:false}")
    private boolean isDiscoveryEnabled;

    @Value("${persistence.strategy:unknown}")
    private String conversationRepositorySource;

    @Value("#{'${persistence.strategy:cassandra}' == \"riak\" ? '${persistence.riak.datacenter.primary.hosts:unknown}'.split(',') : '${persistence.cassandra.endpoint:unknown}'.split(',')}")
    private List<String> conversationRepositoryHosts;

    @Value("#{'${persistence.strategy:cassandra}' == \"riak\" ? '${persistence.riak.bucket.name.prefix:}' : '${persistence.cassandra.keyspace:}'}")
    private String conversationRepositorySchemaOrPrefix;

    @Value("${search.es.clustername:unknown}")
    private String searchClusterName;

    @Value("${persistence.cassandra.dc:unknown}")
    private String cassandraDc;

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

        public List<String> getConversationRepositoryHosts() {
            return conversationRepositoryHosts;
        }

        public String getConversationRepositorySchemaOrPrefix() {
            return conversationRepositorySchemaOrPrefix;
        }

        public String getCassandraDc() {
            return cassandraDc;
        }

        public String getSearchClusterName() {
            return searchClusterName;
        }

        public String getSearchClusterVersion() throws InterruptedException {
            if (searchClient == null) {
                return "unknown";
            }

            try {
                Set<String> versions = new HashSet<>();

                // If versions differ between cluster nodes, return a comma separated list instead

                searchClient.admin().cluster().prepareNodesInfo().execute().get().forEach(info -> versions.add(info.getVersion().toString()));

                return StringUtils.collectionToDelimitedString(versions, ", ");
            } catch (ExecutionException|ElasticsearchException e) {
                return "error";
            }
        }
    }
}
