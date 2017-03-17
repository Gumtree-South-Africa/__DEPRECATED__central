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

/**
 * a {@code MessageProcessedListener} for formatting conversation data to suit data warehouse requirements.
 */
public class DataWarehouseEventLogListener implements MessageProcessedListener {

    private static final String FILTER_NAME = "filterName";

    private static final String FILTER_INSTANCE = "filterInstance";

    private static final Logger LOG = LoggerFactory.getLogger(DataWarehouseEventLogListener.class);

    private static final List<String> EXCLUDED_FILTERS = ImmutableList.of("com.ecg.replyts.app.filterchain.FilterChain",
            "com.ecg.replyts.app.preprocessorchain.preprocessors.AutomatedMailRemover");

    private EventPublisher eventPublisher;

    private Clock clock;

    private final Timer timer = TimingReports.newTimer("datawarehouse-eventlog-process-timer");

    /**
     * Constructor.
     *
     * @param eventPublisher the endpoint to publish events to
     * @param clock          for time based logic
     */
    public DataWarehouseEventLogListener(EventPublisher eventPublisher, Clock clock) {
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        Timer.Context timerContext = null;
        try {
            timerContext = timer.time();
            eventPublisher.publish(event(conversation, message));
        } catch (Exception ex) {
            LOG.error(String.format("Error creating reporting event log payload for conversation %s and message %s",
                    conversation.getId(), message.getId()), ex);
        } finally {
            if (timerContext != null) {
                timerContext.stop();
            }
        }
    }

    private MessageProcessedEvent event(Conversation conversation, Message message) throws Exception {

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

        return builder.createMessageProcessedEvent();
    }

    private boolean isBogusRts2Filter(ProcessingFeedback feedback) {
        return EXCLUDED_FILTERS.contains(feedback.getFilterName());
    }
}
