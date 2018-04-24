package com.ecg.messagecenter.kjca.capi;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.kjca.pushmessage.exception.APIException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static com.ecg.messagecenter.kjca.capi.HttpClientBuilder.buildHttpClient;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

public abstract class InfoLookup<T> {

    protected final HttpClient httpClient;
    protected final HttpHost commonApiHost;
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    private final CredentialsProvider credentialsProvider;

    public InfoLookup(HttpClientConfig httpClientConfig, CommonApiConfig commonApiConfig) {
        this.commonApiHost = commonApiConfig.getCapiEndpoint();
        this.credentialsProvider = new BasicCredentialsProvider();
        this.credentialsProvider.setCredentials(
                new AuthScope(commonApiHost),
                new UsernamePasswordCredentials(commonApiConfig.getUsername(), commonApiConfig.getPassword())
        );

        this.httpClient = buildHttpClient(httpClientConfig);
    }

    private HttpContext getHttpContext() {
        HttpContext context = new BasicHttpContext();
        context.setAttribute(HttpClientContext.CREDS_PROVIDER, credentialsProvider);
        return context;
    }

    protected abstract HttpRequest buildRequest(String requestParam) throws UnsupportedEncodingException;

    public Optional<T> lookupInfo(String requestParamName, String requestParamValue) {
        try {
            HttpRequest request = buildRequest(requestParamValue);
            return httpClient.execute(commonApiHost, request, getResponseHandler(), getHttpContext());
        } catch (IOException e) {
            LOG.error("Error fetching {} for parameter [{}]", requestParamName, e);
            getCounter().inc();
            return Optional.empty();
        }
    }

    protected abstract Counter getCounter();

    protected ResponseHandler<Optional<T>> getResponseHandler() {
        return httpResponse -> {
            int code = httpResponse.getStatusLine().getStatusCode();
            switch (code) {
                case SC_OK:
                    return Optional.of(lookupInfoFromResponse(httpResponse));
                case SC_NOT_FOUND:
                    return Optional.empty();
                default:
                    throw new APIException(EntityUtils.toString(httpResponse.getEntity()));
            }
        };
    }

    protected abstract T lookupInfoFromResponse(HttpResponse response) throws IOException;
}
