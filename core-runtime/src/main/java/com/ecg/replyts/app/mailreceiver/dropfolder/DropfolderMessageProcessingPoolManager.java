package com.ecg.replyts.app.mailreceiver.dropfolder;

import com.ecg.replyts.app.mailreceiver.MessageProcessingPoolManager;
import com.ecg.replyts.app.mailreceiver.MessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "mail.provider.strategy", havingValue = "fs", matchIfMissing = true)
public class DropfolderMessageProcessingPoolManager extends MessageProcessingPoolManager {
    private final int mailProcessingThreads;
    private final MessageProcessor messageProcessor;

    @Autowired
    public DropfolderMessageProcessingPoolManager(@Value("${replyts.threadpool.size:2}") int mailProcessingThreads,

                                                  @Value("${replyts.threadpool.shutdown.await.ms:10000}") long gracefulShutdownTimeoutMs,
                                                  @Qualifier("dropfolderMessageProcessor") MessageProcessor messageProcessor) {
        super(mailProcessingThreads, gracefulShutdownTimeoutMs);
        this.mailProcessingThreads = mailProcessingThreads;
        this.messageProcessor = messageProcessor;
    }

    @Override
    protected Stream<MessageProcessor> createProcessorStream() {
        return Stream.generate(() -> messageProcessor).limit(mailProcessingThreads);
    }
}
