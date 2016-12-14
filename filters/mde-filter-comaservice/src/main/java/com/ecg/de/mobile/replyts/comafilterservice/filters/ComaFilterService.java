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
import java.util.List;

public class ComaFilterService {

    private final static Logger logger = LoggerFactory.getLogger(ComaFilterService.class);

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();


    private final ComaFilterServiceClient client;

    private final boolean active;

    public ComaFilterService(String webserviceUrl, boolean areFiltersActive) {
        client = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(webserviceUrl)
                .setErrorHandler(new FilterServiceErrorHandler())
                .setConverter(new GsonConverter(GSON))
                .setLog(new RestAdapter.Log() {
                    @Override
                    public void log(String s) {
                        logger.info(s);
                    }
                })
                .setClient(new ComaFilterServiceHttpClient())
                .build()
                .create(ComaFilterServiceClient.class);

        active = areFiltersActive;
    }

    ComaFilterService(ComaFilterServiceClient comaFilterServiceClient, boolean areFiltersActive){

        client = comaFilterServiceClient;
        active = areFiltersActive;

    }

    public Collection<String> getFilterResults(ContactMessage message) {
        logger.debug("Processing ContactMessage {}.",message);

        if(!active) {
            /**
             * Filters are not active so we do not have to do anything.
             */
            return Collections.emptySet();
        }

        List<String> matchingFilters = client.getFilterResults(message);

        return matchingFilters;
    }

    /**
     * Sets higher than normal timeouts, since ComaFilter can be slow sometimes.
     * Robert Schumann: "we once already set the timeouts on the varnish (cached.csapi.mobile.rz) to very high values for coma-filter-service, because it’s regularly slow"
     * (source: https://ebayclassifiedsgroup.slack.com/archives/ecg-comaas-mde/p1481622119000044)
     */
    private final class ComaFilterServiceHttpClient extends UrlConnectionClient {
        @Override protected HttpURLConnection openConnection(Request request) throws IOException {
            HttpURLConnection connection = super.openConnection(request);
            connection.setConnectTimeout(5 * 1000);
            connection.setReadTimeout(60 * 1000);
            return connection;
        }
    }
}