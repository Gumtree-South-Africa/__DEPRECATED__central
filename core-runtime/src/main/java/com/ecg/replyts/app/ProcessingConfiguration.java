package com.ecg.replyts.app;

import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.preprocessorchain.PreProcessorManager;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Optional.fromNullable;

class ProcessingConfiguration {


    private static final Logger LOG = LoggerFactory.getLogger(ProcessingConfiguration.class);

    private ProcessingFlow flow;

    private ProcessingFinalizer finalizer;

    @Autowired
    private Guids guids;
    @Autowired(required = false)
    private List<MessageProcessedListener> messageProcessedListeners;
    @Autowired
    private PreProcessorManager preProcessor;
    @Autowired
    private FilterChain filterChain;
    @Autowired
    private PostProcessorChain postProcessor;
    @Autowired
    private MailDeliveryService mailDeliveryService;
    @Autowired
    private MutableConversationRepository conversationRepository;
    @Autowired
    private MailRepository mailRepository;
    @Autowired
    private SearchIndexer searchIndexer;
    @Autowired
    private ConversationEventListeners conversationEventListeners;

    private List<MessageProcessedListener> processedListeners;

    @PostConstruct
    void setup() {
        processedListeners = fromNullable(messageProcessedListeners).or(Collections.<MessageProcessedListener>emptyList());
        StringBuilder foundListenersLogEntry = new StringBuilder("With MessageProcessedListeners: ");
        for (MessageProcessedListener l : processedListeners) {
            foundListenersLogEntry.append(l.getClass()).append(" ");
        }
        LOG.info(foundListenersLogEntry.toString());
        flow = new ProcessingFlow(mailDeliveryService, postProcessor, filterChain, preProcessor);
        finalizer = new ProcessingFinalizer(conversationRepository, mailRepository, searchIndexer, conversationEventListeners);
    }

    /**
     * @param maxMessageProcessingTimeSeconds max allowed processing time, 0 means infinite.
     */
    @Bean
    public MessageProcessingCoordinator messageProcessingCoordinator(@Value("${replyts.maxMessageProcessingTimeSeconds:0}")
                                                                     long maxMessageProcessingTimeSeconds) {
        return new MessageProcessingCoordinator(guids, finalizer, flow, processedListeners, new ProcessingContextFactory(maxMessageProcessingTimeSeconds));
    }

    @Bean
    public ModerationService moderationService() {
        return new DirectMessageModerationService(conversationRepository, flow, mailRepository, searchIndexer, processedListeners, conversationEventListeners);
    }

}
