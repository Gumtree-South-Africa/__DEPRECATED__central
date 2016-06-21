package com.ecg.de.mobile.replyts.badword;

import com.google.gson.Gson;
import de.mobile.cs.filter.domain.BadwordDTO;
import de.mobile.cs.filter.domain.FilterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.POST;
import retrofit.http.Query;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.mobile.cs.filter.domain.BadwordType.SWEARWORD;

@Component
class CsFilterServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CsFilterServiceClient.class);

    private static final BadwordDTO FAILED_BADWORD = BadwordDTO.newBuilder()
            .id("FAILED")
            .term("FAILED")
            .stemmed("FAILED")
            .type(SWEARWORD)
            .build();

    interface Service {

        @POST("/v1/filter")
        FilterResult filterText(@Query("text") String text);

    }

    private final Service filterService;

    CsFilterServiceClient(String endpoint) {
        filterService = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setConverter(new GsonConverter(new Gson()))
                .build()
                .create(Service.class);
    }

    List<BadwordDTO> filterSwearwords(String text) {
        try {
            final FilterResult filterResult = filterService.filterText(text);
            return filterResult.getBadwords().stream()
                    .filter(badword -> badword.getType() == SWEARWORD)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            logger.error("cs filter service failed", ex);
            return Collections.singletonList(FAILED_BADWORD);
        }
    }

}
