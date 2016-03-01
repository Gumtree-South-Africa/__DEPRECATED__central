package com.ecg.de.kleinanzeigen.ruleperformance;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author mhuttar
 */
public class FilterRulePerformanceListener implements MessageProcessedListener {

    private final Logger LOG = LoggerFactory.getLogger("hadoop_filter_performance");

    private final static PerformanceLogLine LINE_BUILDER = new PerformanceLogLine();

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (!LOG.isInfoEnabled()) {
            return;
        }

        List<ProcessingFeedback> processingFeedback = message.getProcessingFeedback();
        Optional<PerformanceLogType> type = PerformanceLogType.valueOf(message);
        if (processingFeedback == null || processingFeedback.isEmpty() || !type.isPresent()) {
            return;
        }

        for (ProcessingFeedback feedback : processingFeedback) {
            LOG.info(LINE_BUILDER.format(type.get(), feedback));
        }


    }
}
