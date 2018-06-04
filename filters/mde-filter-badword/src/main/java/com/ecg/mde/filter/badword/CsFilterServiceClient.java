package com.ecg.mde.filter.badword;

import com.scarabsoft.jrest.JRest;
import com.scarabsoft.jrest.annotation.Param;
import com.scarabsoft.jrest.annotation.Post;
import com.scarabsoft.jrest.converter.GsonConverterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class CsFilterServiceClient {
    private static final Logger LOG = LoggerFactory.getLogger(CsFilterServiceClient.class);

    private static final BadwordDTO FAILED_BADWORD = BadwordDTO.newBuilder()
            .id("FAILED")
            .term("FAILED")
            .stemmed("FAILED")
            .type(BadwordType.SWEARWORD)
            .build();

    interface Service {

        @Post(value = "/v1/filter", multipart = false)
        FilterResult filterText(@Param("text") String text);

    }

    private final Service filterService;

    CsFilterServiceClient(String endpoint) {
        filterService = JRest.newBuilder()
                .baseUrl(endpoint)
                .converterFactory(new GsonConverterFactory())
                .build()
                .create(Service.class);
    }

    List<BadwordDTO> filterSwearwords(String text) {
        try {
            final FilterResult filterResult = filterService.filterText(text);
            return filterResult.getBadwords().stream()
                    .filter(badword -> badword.getType() == BadwordType.SWEARWORD)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            LOG.error("cs filter service failed", ex);
            return Collections.singletonList(FAILED_BADWORD);
        }
    }
}
