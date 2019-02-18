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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_BE;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;

public class BlockImageAttachmentsBeforeSellerRepliesFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(BlockImageAttachmentsBeforeSellerRepliesFilter.class);

    private static final String X_MESSAGE_METADATA = "X-Message-Metadata";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        if (!hasSellerReplied(context.getConversation()) && containsImages(context.getMessage())) {
            return Collections.singletonList(buildFilterRuleViolationFeedback());
        }

        return Collections.emptyList();
    }

    private static FilterFeedback buildFilterRuleViolationFeedback() {
        String feedbackDescription = "Image attachment is not allowed until first seller reply";
        return new FilterFeedback(feedbackDescription, feedbackDescription, null, FilterResultState.DROPPED);
    }

    boolean hasSellerReplied(Conversation conversation) {
        return conversation.getMessages().stream().anyMatch(m -> m.getMessageDirection() == MessageDirection.SELLER_TO_BUYER);
    }

    boolean containsImages(Message currentMessage) {
        Map<String, String> metadata = currentMessage.getCaseInsensitiveHeaders();

        if (!metadata.containsKey(X_MESSAGE_METADATA)) {
            return false;
        }

        String xMessageData = currentMessage.getCaseInsensitiveHeaders().get(X_MESSAGE_METADATA);

        return Try.of(() -> OBJECT_MAPPER.readTree(xMessageData))
                .onFailure(BlockImageAttachmentsBeforeSellerRepliesFilter::logMetadataDecodingException)
                .toJavaOptional()
                .map(rootNode -> rootNode.get("attachment"))
                .map(attachmentNode -> attachmentNode.get("type"))
                .map(typeNode -> "image".equals(typeNode.asText()))
                .orElse(false);
    }

    private static void logMetadataDecodingException(Throwable objectMapperException) {
        LOG.warn("failed to decode the {} metadata value", X_MESSAGE_METADATA, objectMapperException);
    }

    @ComaasPlugin
    @Profile({TENANT_MP, TENANT_BE})
    @Component
    @SuppressWarnings("unused") // Picked up via the component scan
    public static class Factory implements FilterFactory {

        @Nonnull
        @Override
        public Filter createPlugin(String instanceName, JsonNode configuration) {
            return new BlockImageAttachmentsBeforeSellerRepliesFilter();
        }

        @Nonnull
        @Override
        public String getIdentifier() {
            return "com.ecg.unicom.comaas.filter.mp.BlockImageAttachmentsBeforeSellerRepliesFilter";
        }
    }
}
