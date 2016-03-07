package com.ecg.de.mobile.replyts.comafilterservice.filters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

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

}
