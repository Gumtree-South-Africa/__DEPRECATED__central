package com.ecg.gumtree.comaas.filter.volume;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.gumtree.common.util.time.Clock;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

class VolumeFilterServiceHelper {

    private Clock clock;

    VolumeFilterServiceHelper(Clock clock) {
        this.clock = clock;
    }

    /**
     * Create the search parameters to find the number of messages sent in the past X seconds.
     *
     * @param field   the field to identify the user by
     * @param value   the value to identify the user by
     * @param seconds the number of seconds back to check
     * @return the parameters to use for this search
     */
    SearchMessagePayload createSearchParameters(VelocityFilterConfig.FilterField field, String value, int seconds) {
        DateTime startDate = clock.getDateTime().minusSeconds(seconds);
        SearchMessagePayload searchMessagePayload = new SearchMessagePayload();
        searchMessagePayload.setFromDate(startDate.toDate());
        Map<String, String> customValues = new HashMap<>();

        switch (field) {
            case EMAIL:
                searchMessagePayload.setUserEmail(value);
                searchMessagePayload.setUserRole(SearchMessagePayload.ConcernedUserRole.SENDER);
                break;
            case COOKIE:
                customValues.put(ElasticsearchCustomHeaderKeys.BUYER_COOKIE.getCustomHeaderKey(), value);
                searchMessagePayload.setConversationCustomValues(customValues);
                break;
            case IP_ADDRESS:
                customValues.put(ElasticsearchCustomHeaderKeys.BUYER_IP.getCustomHeaderKey(), value);
                searchMessagePayload.setConversationCustomValues(customValues);
                break;
        }

        return searchMessagePayload;
    }

    /**
     * Create the search parameters to find the number of manually checked messages sent in the past X seconds.
     *
     * @param field   the field to identify the user by
     * @param value   the value to identify the user by
     * @param seconds the number of seconds back to check
     * @return the parameters to use for this search
     */
    SearchMessagePayload createWhitelistSearchParameters(VelocityFilterConfig.FilterField field, String value, int seconds) {
        SearchMessagePayload searchMessagePayload = createSearchParameters(field, value, seconds);
        searchMessagePayload.setHumanResultState(ModerationResultState.GOOD);
        searchMessagePayload.setCount(1);
        return searchMessagePayload;
    }
}
