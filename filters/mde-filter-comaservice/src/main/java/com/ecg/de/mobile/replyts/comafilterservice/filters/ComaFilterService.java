package com.ecg.de.mobile.replyts.comafilterservice.filters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.RestAdapter;
import retrofit.client.Request;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.GsonConverter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Collections;

public class ComaFilterService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ComaFilterService.class);

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();


    private final ComaFilterServiceClient client;

    private final boolean active;
    private final int connectTimeout;
    private final int readTimeout;

    public ComaFilterService(String webserviceUrl, boolean areFiltersActive, int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        client = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(webserviceUrl)
                .setErrorHandler(new FilterServiceErrorHandler())
                .setConverter(new GsonConverter(GSON))
                .setLog(LOGGER::info)
                .setClient(new ComaFilterServiceHttpClient())
                .build()
                .create(ComaFilterServiceClient.class);

        active = areFiltersActive;
    }

    ComaFilterService(ComaFilterServiceClient comaFilterServiceClient, boolean areFiltersActive) {
        connectTimeout = 5000;
        readTimeout = 60000;
        client = comaFilterServiceClient;
        active = areFiltersActive;

    }

    public Collection<String> getFilterResultsForMessage(ContactMessage message) {
        LOGGER.debug("Processing message ContactMessage {}.", message);
        if (!active) {
            return Collections.emptySet();
        }
        return client.getFilterResultsForMessage(message);
    }

    public Collection<String> getFilterResultsForConversation(ContactMessage message) {
        LOGGER.debug("Processing conversation ContactMessage {}.", message);
        if (!active) {
            return Collections.emptySet();
        }
        return client.getFilterResultsForConversation(message);
    }

    /**
     * Sets higher than normal timeouts, since ComaFilter can be slow sometimes.
     * Robert Schumann: "we once already set the timeouts on the varnish (cached.csapi.mobile.rz) to very high values for coma-filter-service, because it’s regularly slow"
     * (source: https://ebayclassifiedsgroup.slack.com/archives/ecg-comaas-mde/p1481622119000044)
     */
    private final class ComaFilterServiceHttpClient extends UrlConnectionClient {
        @Override
        protected HttpURLConnection openConnection(Request request) throws IOException {
            HttpURLConnection connection = super.openConnection(request);
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            return connection;
        }
    }
}