package com.gumtree.replyts2.plugins.reporting;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.util.Clock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * a {@code MessageProcessedListener} for formatting conversation data to suit data warehouse requirements.
 */
public class DataWarehouseEventLogListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(DataWarehouseEventLogListener.class);

    private static final String FILTER_NAME = "filterName";

    private static final String FILTER_INSTANCE = "filterInstance";

    private static final List<String> EXCLUDED_FILTERS = ImmutableList.of("com.ecg.replyts.app.filterchain.FilterChain",
            "com.ecg.replyts.app.preprocessorchain.preprocessors.AutomatedMailRemover");

    private EventPublisher eventPublisher;

    private Clock clock;

    private final Timer timer = TimingReports.newTimer("datawarehouse-eventlog-process-timer");

    public DataWarehouseEventLogListener(EventPublisher eventPublisher, Clock clock) {
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        try (Timer.Context ignore = timer.time()) {
            event(conversation, message).ifPresent(eventPublisher::publish);
        } catch (Exception e) {
            LOG.error("Error creating reporting event log payload for conversation {} and message {}", conversation.getId(), message.getId(), e);
        }
    }

    private Optional<MessageProcessedEvent> event(Conversation conversation, Message message) throws Exception {
        // Often, the conversation.getAdId() call returns null. This is a known error that is also happening on RTS2 in GTUK.
        if (conversation.getAdId() == null) {
            return Optional.empty();
        }

        MessageProcessedEvent.Builder builder = new MessageProcessedEvent.Builder()
                .messageId(message.getId())
                .conversationId(conversation.getId())
                .messageDirection(message.getMessageDirection())
                .conversationState(conversation.getState())
                .messageState(message.getState())
                .filterResultState(message.getFilterResultState())
                .humanResultState(message.getHumanResultState())
                .adId(Long.parseLong(conversation.getAdId()))
                .sellerMail(conversation.getSellerId())
                .buyerMail(conversation.getBuyerId())
                .numOfMessageInConversation(conversation.getMessages().indexOf(message))
                .timestamp(new DateTime(clock.now().getTime()))
                .conversationCreatedAt(conversation.getCreatedAt())
                .messageReceivedAt(message.getReceivedAt())
                .conversationLastModifiedAt(conversation.getLastModifiedAt())
                .messageLastModifiedAt(message.getLastModifiedAt())
                .customCategoryId(Integer.parseInt(conversation.getCustomValues().get("categoryid")))
                .customBuyerIp(conversation.getCustomValues().get("buyerip"));

        if (message.getProcessingFeedback() != null) {
            for (ProcessingFeedback processingFeedback : message.getProcessingFeedback()) {
                if (!isBogusRts2Filter(processingFeedback)) {
                    String processingFeedbackDescription = processingFeedback.getDescription();
                    JsonNode pfRoot = new ObjectMapper().readTree(processingFeedbackDescription);

                    FilterResultState resultState = processingFeedback.getResultState();
                    builder.addFilterResult(new FilterExecutionResult.Builder()
                            .filterName(pfRoot.get(FILTER_NAME).textValue())
                            .filterInstance(pfRoot.get(FILTER_INSTANCE).textValue())
                            .uiHint(processingFeedback.getUiHint())
                            .score(processingFeedback.getScore())
                            .evaluation(processingFeedback.isEvaluation())
                            .resultState(resultState.name())
                    );
                }
            }
        }

        return Optional.of(builder.createMessageProcessedEvent());
    }

    private boolean isBogusRts2Filter(ProcessingFeedback feedback) {
        return EXCLUDED_FILTERS.contains(feedback.getFilterName());
    }
}
