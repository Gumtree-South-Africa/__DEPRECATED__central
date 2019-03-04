package com.ecg.unicom.comaas.filter.mp;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.ecg.comaas.mp.postprocessor.urlgateway.support.PlainTextMailPartUrlGatewayRewriter.URL_PATTERN;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_BE;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;

public class BlockUrlMessageBeforeSellerRepliesFilter implements Filter {

    /*
    We use X-User-Message header as it contains the message text as is displayed in the MP chat.
    This is used instead of plainBodyText which, for specific first message types (e.g. ASQs and bids),
    contains email specific formatting, including urls, which would then be blocked by this filter.
     */
    private static final String X_USER_MESSAGE = "X-User-Message";

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        if (!hasSellerReplied(context.getConversation()) && containsUrls(context.getMessage())) {
            return Collections.singletonList(buildFilterRuleViolationFeedback());
        }

        return Collections.emptyList();
    }

    private static boolean hasSellerReplied(Conversation conversation) {
        return conversation.getMessages().stream().anyMatch(m -> m.getMessageDirection() == MessageDirection.SELLER_TO_BUYER);
    }

    private static FilterFeedback buildFilterRuleViolationFeedback() {
        String feedbackDescription = "Urls are not allowed until first seller reply";
        return new FilterFeedback(feedbackDescription, feedbackDescription, null, FilterResultState.DROPPED);
    }

    boolean containsUrls(Message currentMessage) {

        Map<String, String> metadata = currentMessage.getCaseInsensitiveHeaders();

        if (!metadata.containsKey(X_USER_MESSAGE)) {
            return false;
        }

        String xUserMessage = metadata.get(X_USER_MESSAGE);

        Matcher matcher = URL_PATTERN.matcher(xUserMessage);
        return matcher.find();
    }

    @ComaasPlugin
    @Profile({TENANT_MP, TENANT_BE})
    @Component
    @SuppressWarnings("unused") // Picked up via the component scan
    public static class Factory implements FilterFactory {

        @Nonnull
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            return new BlockUrlMessageBeforeSellerRepliesFilter();
        }

        @Nonnull
        @Override
        public String getIdentifier() {
            return "com.ecg.unicom.comaas.filter.mp.BlockUrlMessageBeforeSellerRepliesFilter";
        }
    }
}