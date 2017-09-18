package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class ConfigurationHealthCommand extends AbstractHealthCommand {

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

    @Value("#{'${persistence.cassandra.core.endpoint:unknown}'.split(',')}")
    private List<String> cassandraHosts;

    @Value("#{'${search.es.endpoints:unknown}'.split(',')}")
    private List<String> elasticSearchHosts;

    @Value("${persistence.riak.bucket.name.prefix:}")
    private String riakBucketPrefix;

    @Value("${persistence.cassandra.core.keyspace:replyts2}")
    private String cassandraKeyspace;

    @Value("${search.es.clustername:unknown}")
    private String searchClusterName;

    @Value("${persistence.cassandra.core.dc:#{systemEnvironment['region']}}")
    private String cassandraDc;

    private Client searchClient;

    ConfigurationHealthCommand(Client searchClient) {
        this.searchClient = searchClient;
    }

    @PostConstruct
    public void init() throws InterruptedException {
        this.searchClusterVersion = ElasticsearchHealthCommand.getSearchClusterVersion(searchClient);
        this.hostname = getHostname();
    }

    @Override
    public ObjectNode execute() {
        return JsonObjects.builder()
                .attr("version", version)
                .attr("instanceName", instanceName)
                .attr("tenant", tenant)
                .attr("discoveryEnabled", isDiscoveryEnabled)
                .attr("conversationRepositorySource", conversationRepositorySource)
                .attr("elasticSearchHosts", elasticSearchHosts)
                .attr("riakHosts", riakHosts)
                .attr("cassandraHosts", cassandraHosts)
                .attr("riakBucketPrefix", riakBucketPrefix)
                .attr("cassandraKeyspace", cassandraKeyspace)
                .attr("cassandraDc", cassandraDc)
                .attr("searchClusterName", searchClusterName)
                .attr("searchClusterVersion", searchClusterVersion)
                .attr("hostname", hostname)
                .build();
    }

    @Override
    public String name() {
        return "configuration";
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return UNKNOWN;
        }
    }
}
