package com.ecg.replyts.migrations.cleanupoptimizer;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.basho.riak.client.builders.RiakObjectBuilder.newBuilder;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;

public class ConversationMigrator {

    private static final Logger LOG = LoggerFactory.getLogger("migrations");
    public static final String CONVERSATION_SECRETS_BUCKET = "conversation_secrets";
    private static final String MAIL_BUCKET = "mail";
    private static final String CONV_CREATED_INDEX = "conv_created";

    static final String REPLYTS_MAIL_MANIFEST = "replyts/mail-manifest";
    public static final String MAIL_RECEIVED_INDEX = "created";


    private final ConversationRepository repository;

    private final Bucket secretBucket;
    private final Bucket mailBucket;

    @Autowired
    public ConversationMigrator(ConversationRepository repository, IRiakClient riakClient) throws RiakRetryFailedException {
        this.repository = repository;

        secretBucket = riakClient.fetchBucket(CONVERSATION_SECRETS_BUCKET).withRetrier(new DefaultRetrier(3)).execute();
        mailBucket = riakClient.fetchBucket(MAIL_BUCKET).withRetrier(new DefaultRetrier(3)).execute();
    }

    public void migrate(String conversationId) {
        try {
            LOG.info("REPAIR: " + conversationId);
            MutableConversation conversation = repository.getById(conversationId);

            LOG.info(" - conversation={}, seller-secrect={}", conversation.getId(), conversation.getSellerSecret());
            updateSecret(conversation, conversation.getSellerSecret());

            LOG.info(" - conversation={}, buyer-secrect={}", conversation.getId(), conversation.getBuyerSecret());
            updateSecret(conversation, conversation.getBuyerSecret());

            for (Message message : conversation.getMessages()) {
                LOG.info(" - conversation={}, message={}", conversation.getId(), message.getId());
                convertMail(conversation, message);
            }

        } catch (Exception e) {
            LOG.warn("FAILED " + conversationId, e);
        }
    }

    private void convertMail(MutableConversation conversation, Message message) throws RiakRetryFailedException {
        IRiakObject object = fetchMailObject(message.getId());
        if (REPLYTS_MAIL_MANIFEST.equals(object.getContentType())) {
            LOG.info("   * conversation={} manifest={}", conversation.getId(), message.getId());
            updateMailObject(conversation, message, object, message.getId());
            List<String> manifestChunks = parseManifest(object.getValueAsString());
            for (String manifestChunk : manifestChunks) {
                LOG.info("   * conversation={} chunk={}", conversation.getId(), message.getId());
                updateMailObject(conversation, message, fetchMailObject(manifestChunk),manifestChunk);
            }

        } else {
            updateMailObject(conversation, message, object, message.getId());
        }


    }

    private IRiakObject fetchMailObject(String messageId) throws RiakRetryFailedException {
        return mailBucket.fetch(messageId).r(1).notFoundOK(false).execute();
    }

    private void updateMailObject(MutableConversation conversation, Message message, IRiakObject object, String id) throws RiakRetryFailedException {
        mailBucket.store(newBuilder(MAIL_BUCKET, id)
                .withValue(object.getValue())
                .withContentType(object.getContentType())
                .addIndex(MAIL_RECEIVED_INDEX, timestampInMinutes(message.getReceivedAt()))
                .addIndex(CONV_CREATED_INDEX, timestampInMinutes(conversation.getCreatedAt()))
                .build()
        ).w(Quora.QUORUM).returnBody(false).execute();
    }

    private void updateSecret(Conversation conv, String secret) throws RiakRetryFailedException {
        if(conv.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            LOG.info("SKIP null secret conversation=" + conv.getId());
            return;
        }

        if(Strings.isNullOrEmpty(secret)) {
            LOG.info("SKIP null secret conversation=" + conv.getId());
            return;
        }

        IRiakObject obj = secretBucket.fetch(secret).r(1).notFoundOK(false).execute();

        secretBucket.store(
                newBuilder(CONVERSATION_SECRETS_BUCKET, secret)
                        .withContentType(obj.getContentType())
                        .withValue(obj.getValue())
                        .addIndex(CONV_CREATED_INDEX, timestampInMinutes(conv.getCreatedAt()))
                        .build())
                .w(Quora.QUORUM)
                .returnBody(false)
                .execute();

    }


    private List<String> parseManifest(String json) {
        List<String> chunkKeys = Lists.newArrayList();
        ArrayNode arr = (ArrayNode) JsonObjects.parse(json).get("chunks");
        for (JsonNode node : arr) {
            chunkKeys.add(node.asText());
        }
        return chunkKeys;

    }
}
