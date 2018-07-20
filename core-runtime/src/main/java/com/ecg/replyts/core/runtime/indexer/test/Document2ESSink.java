package com.ecg.replyts.core.runtime.indexer.test;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.runtime.indexer.DocumentSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*
Should only be used in tests
 */
public class Document2ESSink implements DocumentSink {
    private static final Logger LOG = LoggerFactory.getLogger(Document2ESSink.class);

    DirectESIndexer directESIndexer;
    private int timeout = 5;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    Document2ESSink(@Autowired DirectESIndexer directESIndexer) {
        this.directESIndexer = directESIndexer;
    }

    @Override
    public void sink(List<Conversation> conversations) {
        try {
            directESIndexer.ensureIndexed(conversations, timeout, timeUnit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void sink(Conversation conversation) {
        try {
            directESIndexer.ensureIndexed(conversation, timeout, timeUnit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sink(Conversation conversation, String ignored) {
        sink(conversation);
    }

    @PostConstruct
    private void report() {
        LOG.info("Direct sink to ES is ENABLED. (YOU MUST BE IN TEST MODE)");
    }
}
