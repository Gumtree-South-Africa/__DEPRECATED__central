package com.ecg.gumtree.comaas.common.filter;

import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.client.impl.GumshieldClientExecutorFactory;
import com.gumtree.gumshield.api.client.spec.*;
import com.gumtree.gumshield.api.client.util.JacksonContextResolver;
import com.gumtree.healthcheck.core.Health;
import com.gumtree.healthcheck.core.HealthCheck;
import com.gumtree.healthcheck.core.HealthCheckRegistrator;
import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implementation of {@link com.gumtree.gumshield.api.client.GumshieldApi} that looks up a
 * remote Gumshield API service endpoint.
 */
public final class RemoteGumshieldApi implements GumshieldApi, HealthCheck {

    private static ResteasyProviderFactory factory;

    static {
        // Make sure we always get a fresh factory, to avoid picking up the one created by SAPI, for example
        ResteasyProviderFactory.setInstance(null);
        factory = ResteasyProviderFactory.getInstance();
        factory.registerProviderInstance(new JacksonContextResolver());
        factory.registerProviderInstance(new RequestResponseFilter());
    }

    private final String id;
    private final URI baseUri;
    private final GumshieldClientExecutorFactory clientExecutorFactory;

    private HealthCheckApi healthCheckApi;
    private AdvertApi advertApi;
    private AgentApi agentApi;
    private ChecklistApi checklistApi;
    private UserApi userApi;
    private UserReportApi userReportApi;
    private RefundApi refundApi;
    private ConversationApi conversationApi;
    private CsReviewApi csReviewApi;
    private CsMessageReviewApi csMessageReviewApi;

    /**
     * Constructor
     *
     * @param baseUri               the base uri
     * @param clientExecutorFactory for creating client executors
     * @throws URISyntaxException - throw an error if we cannot parse the URI provided
     */
    public RemoteGumshieldApi(String baseUri, GumshieldClientExecutorFactory clientExecutorFactory) throws URISyntaxException {
        this("gumshield-api", baseUri, clientExecutorFactory);
    }

    public RemoteGumshieldApi(String id, String baseUri, GumshieldClientExecutorFactory clientExecutorFactory) throws URISyntaxException {
        this.id = id;
        this.baseUri = new URI(baseUri);
        this.clientExecutorFactory = clientExecutorFactory;

        advertApi = ProxyFactory.create(AdvertApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        agentApi = ProxyFactory.create(AgentApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        checklistApi = ProxyFactory.create(ChecklistApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        userApi = ProxyFactory.create(UserApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        userReportApi = ProxyFactory.create(UserReportApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        refundApi = ProxyFactory.create(RefundApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        conversationApi = ProxyFactory.create(ConversationApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        csReviewApi = ProxyFactory.create(CsReviewApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        csMessageReviewApi = ProxyFactory.create(CsMessageReviewApi.class, this.baseUri, clientExecutorFactory.create(), factory);
        healthCheckApi = ProxyFactory.create(HealthCheckApi.class, getNoApiUri(baseUri), clientExecutorFactory.create(), factory);

        HealthCheckRegistrator.register(this);
    }

    private URI getNoApiUri(String baseUri) throws URISyntaxException {
        return new URI(baseUri.replaceAll("/api", ""));
    }

    @Override
    public AdvertApi advertApi() {
        return advertApi;
    }

    @Override
    public AgentApi agentApi() {
        return agentApi;
    }

    @Override
    public ChecklistApi checklistApi() {
        return checklistApi;
    }

    @Override
    public UserApi userApi() {
        return userApi;
    }

    @Override
    public UserReportApi userReportApi() {
        return userReportApi;
    }

    @Override
    public RefundApi refundApi() {
        return refundApi;
    }

    @Override
    public ConversationApi conversationApi() {
        return conversationApi;
    }

    @Override
    public CsReviewApi csReviewApi() {
        return csReviewApi;
    }

    @Override
    public CsMessageReviewApi csMessageReviewApi() {
        return csMessageReviewApi;
    }

    @Override
    public <T extends ApiBase> T create(Class<T> apiClass) {
        return ProxyFactory.create(
                apiClass,
                baseUri,
                clientExecutorFactory.create(), factory);

    }

    public String getBaseUri() {
        return baseUri.toString();
    }

    public GumshieldClientExecutorFactory getClientExecutorFactory() {
        return clientExecutorFactory;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRemote() {
        return baseUri.toASCIIString();
    }

    @Override
    public Health getHealth() {
        try {
            String health = healthCheckApi.getHealth();
            return "{\"status\":\"healthy\"}".equals(health) ? Health.healthy() : Health.unhealthy(health);
        } catch (Exception e) {
            return Health.unhealthy(e);
        }
    }
}
