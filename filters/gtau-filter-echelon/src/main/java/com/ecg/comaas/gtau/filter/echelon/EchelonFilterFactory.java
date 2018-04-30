package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Component
public class EchelonFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ebay.replyts.australia.echelon.EchelonFilterFactory";

    private static final Logger LOG = LoggerFactory.getLogger(EchelonFilterFactory.class);

    // used in case we create a couple of EchelonFilter instances with different timeout configuration
    private final List<CloseableHttpClient> httpClients = new ArrayList<>();
    private final int maxConnectionsPerHost;
    private final int maxTotalConnections;

    @Autowired
    public EchelonFilterFactory(
            @Value("${comaas.echelon.filter.http.maxConnectionsPerHost:100}") int maxConnectionsPerHost,
            @Value("${comaas.echelon.filter.http.maxTotalConnections:100}") int maxTotalConnections
    ) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        this.maxTotalConnections = maxTotalConnections;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode jsonNode) {
        try {
            EchelonFilterConfiguration configuration = EchelonFilterPatternRulesParser.fromJson(jsonNode);
            return new EchelonFilter(configuration, getHttpClient(configuration.getEndpointTimeout()));
        } catch (IllegalArgumentException e) {
            LOG.error("Provided JSON configuration is incorrect - '{}'", jsonNode, e);
            throw e;
        }
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    private CloseableHttpClient getHttpClient(int endpointTimeout) {
        CloseableHttpClient httpClient = HttpClientFactory.createCloseableHttpClient(endpointTimeout, endpointTimeout,
                endpointTimeout, maxConnectionsPerHost, maxTotalConnections);
        httpClients.add(httpClient);
        return httpClient;
    }

    @PreDestroy
    public void preDestroy() {
        for (CloseableHttpClient httpClient : httpClients) {
            HttpClientFactory.closeWithLogging(httpClient);
        }
    }
}
