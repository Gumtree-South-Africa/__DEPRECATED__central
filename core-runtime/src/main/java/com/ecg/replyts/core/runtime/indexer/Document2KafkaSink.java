package com.ecg.replyts.core.runtime.indexer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.ecg.replyts.app.ProcessingFinalizer.KAFKA_KEY_FIELD_SEPARATOR;


@Component
@ConditionalOnExpression("#{'${doc2kafka.sink.enabled:true}' == 'true'}")
public class Document2KafkaSink implements DocumentSink {

    private static final Logger LOG = LoggerFactory.getLogger(Document2KafkaSink.class);
    private static final Counter DOC_COUNT = TimingReports.newCounter("document2KafkaSink.document");
    private static final Timer SAVE2KAFKA_TIMER = TimingReports.newTimer("save-chunk2kafka");

    @Autowired
    private IndexDataBuilder indexDataBuilder;

    @Autowired
    @Qualifier("esSink")
    private KafkaSinkService documentSink;

    @Value("${replyts.tenant}")
    private String tenant;

    @Override
    public void sink(List<Conversation> conversations) {
        try (Timer.Context ignore = SAVE2KAFKA_TIMER.time()) {
            conversations.stream().filter(Objects::nonNull).forEach(this::sink);
        }
    }

    @Override
    public void sink(Conversation conversation) {
        conversation.getMessages().stream().map(Message::getId).forEach(id -> this.sink(conversation, id));
    }

    @Override
    public void sink(Conversation conversation, String messageId) {
        try {
            Message message = conversation.getMessageById(messageId);
            XContentBuilder indexData = indexDataBuilder.toIndexData(conversation, message);
            String document = indexData.string();
            String key = tenant + KAFKA_KEY_FIELD_SEPARATOR
                    + conversation.getId() + KAFKA_KEY_FIELD_SEPARATOR
                    + message.getId();
            DOC_COUNT.inc();
            documentSink.storeAsync(key, document.getBytes());

        } catch (IOException e) {
            LOG.error("Failed to store document data in Kafka due to {}", e.getMessage(), e);
        }
    }

    @PostConstruct
    private void report() {
        LOG.info("Document2Kafka sink is activated");
    }

}