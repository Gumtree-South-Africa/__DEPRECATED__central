package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.IndexData;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;

import static com.ecg.replyts.app.ProcessingFinalizer.KAFKA_KEY_FIELD_SEPARATOR;

@Component
public class Document2KafkaSink {

    private static final Logger LOG = LoggerFactory.getLogger(Document2KafkaSink.class);
    private static final Counter DOC_COUNT = TimingReports.newCounter("document2KafkaSink.document");

    @Autowired
    private SearchIndexer searchIndexer;

    @Autowired(required = false)
    @Qualifier("esSink")
    private KafkaSinkService documentSink;

    @Value("${replyts.tenant}")
    private String tenant;

    @PostConstruct
    private void reportConfiguration() {
        if(documentSink!=null) {
            LOG.info("es2kafka sink is ENABLED");
        } else {
            LOG.info("es2kafka sink is DISABLED");
        }
    }



    public void pushToKafka(List<Conversation> conversations) {
        if (documentSink == null) {
            return;
        }
        for (Conversation conversation : conversations) {
            pushToKafka(conversation);
        }
    }

    public void pushToKafka(Conversation conversation) {
        if (documentSink == null) {
            return;
        }
        conversation.getMessages().stream().map(Message::getId).forEach(id -> this.pushToKafka(conversation,id));
    }

    public void pushToKafka(Conversation conversation, String messageId) {
        if (documentSink == null) {
            return;
        }

        try {
            Message message = conversation.getMessageById(messageId);
            IndexData indexData = searchIndexer.getIndexDataBuilder().toIndexData(conversation, message);
            byte[] document = indexData.getDocument().bytes().toBytesRef().bytes;
            String key = tenant + KAFKA_KEY_FIELD_SEPARATOR
                    + conversation.getId() + KAFKA_KEY_FIELD_SEPARATOR
                    + message.getId();
            DOC_COUNT.inc();
            documentSink.storeAsync(key, document);

        } catch (IOException e) {
            LOG.error("Failed to store document data in Kafka due to {}", e.getMessage(), e);
        }
    }

}