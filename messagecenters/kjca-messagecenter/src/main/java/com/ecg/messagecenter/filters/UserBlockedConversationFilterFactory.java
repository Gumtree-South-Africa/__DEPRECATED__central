package com.ecg.messagecenter.filters;

import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserBlockedConversationFilterFactory implements FilterFactory {
    private static final String SCORE_JSON_KEY = "score";

    private final ConversationBlockRepository conversationBlockRepository;

    @Autowired
    public UserBlockedConversationFilterFactory(ConversationBlockRepository conversationBlockRepository) {
        this.conversationBlockRepository = conversationBlockRepository;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        final JsonNode scoreJsonNode = configuration.get(SCORE_JSON_KEY);
        if (scoreJsonNode == null || !scoreJsonNode.canConvertToInt()) {
            throw new IllegalStateException(SCORE_JSON_KEY + " must exist and must be an integer in configuration of "
                    + UserBlockedConversationFilter.class.getName());
        }

        return new UserBlockedConversationFilter(conversationBlockRepository, scoreJsonNode.asInt());
    }
}
