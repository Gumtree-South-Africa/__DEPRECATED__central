package com.ecg.comaas.ebayk.listener.dailyreport.ruleperformance;

import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopEventEmitter;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopLogEntry;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.TrackerLogStyleUseCase;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;

import java.util.List;
import java.util.Optional;

public class FilterRulePerformanceListener implements MessageProcessedListener {
    private static final PerformanceLogLine LINE_BUILDER = new PerformanceLogLine();
    private final HadoopEventEmitter hadoopEmitter;

    public FilterRulePerformanceListener(HadoopEventEmitter hadoopEmitter) {
        this.hadoopEmitter = hadoopEmitter;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        List<ProcessingFeedback> processingFeedback = message.getProcessingFeedback();
        Optional<PerformanceLogType> type = PerformanceLogType.valueOf(message);
        if (processingFeedback == null || processingFeedback.isEmpty() || !type.isPresent()) {
            return;
        }

        for (ProcessingFeedback feedback : processingFeedback) {
            String msg = LINE_BUILDER.format(type.get(), feedback);
            HadoopLogEntry entry = new HadoopLogEntry(TrackerLogStyleUseCase.FILTER_PERFORMANCE_V2, msg);
            hadoopEmitter.insert(entry);
        }
    }
}
