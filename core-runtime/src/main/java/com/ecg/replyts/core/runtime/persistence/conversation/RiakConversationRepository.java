/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.replay;

/**
 *
 */
public class RiakConversationRepository implements MutableConversationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiakConversationRepository.class);

    private static final String DEFAULT_BUCKET_NAME = "conversation";
    private static final String DEFAULT_SECRET_BUCKET_NAME = "conversation_secrets";
    private static final String DEFAULT_INDEX_BUCKET_NAME = "conversation_index";


    private final ConversationBucket conversationBucket;
    private final ConversationSecretBucket conversationSecretBucket;
    private final ConversationIndexBucket conversationIndexBucket;


    private final Timer getByIdTimer = TimingReports.newTimer("conversationRepo-getById");
    private final Timer getBySecretTimer = TimingReports.newTimer("conversationRepo-getBySecret");
    private final Timer isSecretAvailableTimer = TimingReports.newTimer("conversationRepo-isSecretAvailable");
    private final Timer commitTimer = TimingReports.newTimer("conversationRepo-commit");
    private final Timer modifiedBetweenTimer = TimingReports.newTimer("conversationRepo-modifiedBetween");
    private final Timer modifiedBeforeTimer = TimingReports.newTimer("conversationRepo-modifiedBefore");
    private final Timer createdBetweenTimer = TimingReports.newTimer("conversationRepo-createdBetween");


    public RiakConversationRepository(IRiakClient riakClient) {
        this(riakClient, DEFAULT_BUCKET_NAME, DEFAULT_SECRET_BUCKET_NAME, DEFAULT_INDEX_BUCKET_NAME);
    }
    public RiakConversationRepository(IRiakClient riakClient, String bucketNamePrefix) {
    	this(riakClient,bucketNamePrefix + DEFAULT_BUCKET_NAME, bucketNamePrefix + DEFAULT_SECRET_BUCKET_NAME, bucketNamePrefix + DEFAULT_INDEX_BUCKET_NAME );
        }
    
    public RiakConversationRepository(IRiakClient riakClient, String bucketName, String secretBucketName, String indexBucketName) {
        this(new ConversationSecretBucket(riakClient, secretBucketName), new ConversationBucket(riakClient, bucketName), new ConversationIndexBucket(riakClient, indexBucketName));
    }

    RiakConversationRepository(ConversationSecretBucket conversationSecretBucket, ConversationBucket conversationBucket, ConversationIndexBucket conversationIndexBucket) {
        this.conversationSecretBucket = conversationSecretBucket;
        this.conversationBucket = conversationBucket;
        this.conversationIndexBucket = conversationIndexBucket;
    }

    @Override
    public MutableConversation getById(String conversationId) {
        final Timer.Context context = getByIdTimer.time();
        try {
            ConversationEvents conversationEvents = conversationBucket.byId(conversationId);
            if (conversationEvents == null) {
                return null;
            }
            return new DefaultMutableConversation(replay(conversationEvents.getEvents()));

        } finally {
            context.stop();
        }
    }


    @Override
    public MutableConversation getBySecret(String secret) {
        final Timer.Context context = getBySecretTimer.time();
        try {
            NewConversationCommand replayedEvent = getConversationIdForSecretReadRepairAware(secret);
            if (replayedEvent == null) {
                return null;
            }
            MutableConversation foundConversation = getById(replayedEvent.getConversationId());
            if (foundConversation == null) {
                // see https://github.scm.corp.ebay.com/ReplyTS/replyts2-core/wiki/Two-datacenter-operations
                LOGGER.warn("could not load conversation {}, even tough there is a conversation secret referring to it: {}. " +
                        "Recreating empty conversation (this is normal behaviour if you use two datacenters and have a " +
                        "connection loss between them. if this is not the case right now, this might be an indicator for something going wrong)",
                        replayedEvent.getConversationId(), secret);

                return DefaultMutableConversation.create(replayedEvent);
            } else {
                return foundConversation;
            }

        } finally {
            context.stop();
        }
    }

    private NewConversationCommand getConversationIdForSecretReadRepairAware(String secret) {
        // on datacenter connection loss, first read to the bucket will return not found (if quorum criteria are not met)
        // that will call read repair and the second read will succeed.

        NewConversationCommand conversationId = conversationSecretBucket.findConversationId(secret);
        if (conversationId != null) {
            return conversationId;
        }
        try {
            TimeUnit.SECONDS.sleep(1);
            // TODO: Add metrics counter here!
            return conversationSecretBucket.findConversationId(secret);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean isSecretAvailable(String secret) {
        final Timer.Context context = isSecretAvailableTimer.time();
        try {
            return conversationSecretBucket.findConversationId(secret) == null;
        } finally {
            context.stop();
        }
    }

    @Override
    public void commit(String conversationId, List<ConversationEvent> toBeCommittedEvents) {
        final Timer.Context context = commitTimer.time();
        try {
            // default empty conversation event will be copied and populated in the mutator.
            setupConversationIfNewlyCreated(toBeCommittedEvents);
            conversationBucket.write(conversationId, toBeCommittedEvents);
        } finally {
            context.stop();
        }
    }

    private void setupConversationIfNewlyCreated(List<ConversationEvent> toBeCommittedEvents) {
        for (ConversationEvent e : toBeCommittedEvents) {
            if (e instanceof ConversationCreatedEvent) {
                ConversationCreatedEvent createdEvent = ((ConversationCreatedEvent) e);
                if (createdEvent.getState() != ConversationState.DEAD_ON_ARRIVAL) {
                    conversationSecretBucket.persist(createdEvent);
                    conversationIndexBucket.persist(createdEvent);
                }
            }
        }
    }

    @Override
    public List<String> listConversationsModifiedBetween(DateTime start, DateTime end) {
        try (Timer.Context ignore = modifiedBetweenTimer.time()) {
            return conversationBucket.modifiedBetween(start, end);
        }
    }

    @Override
    public Stream<String> streamConversationsModifiedBetween(DateTime start, DateTime end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listConversationsModifiedBefore(DateTime before, int maxResults) {
        try (Timer.Context ignore = modifiedBeforeTimer.time()) {
            return conversationBucket.modifiedBefore(before, maxResults);
        }
    }

    @Override
    public List<String> listConversationsCreatedBetween(DateTime start, DateTime end) {
        try (Timer.Context ignore = createdBetweenTimer.time()) {
            return conversationBucket.createdBetween(start, end);
        }
    }

    @Override
    public void deleteConversation(Conversation c) {
        if (c.getState() != ConversationState.DEAD_ON_ARRIVAL) {
            conversationSecretBucket.delete(c.getBuyerSecret());
            conversationSecretBucket.delete(c.getSellerSecret());

            ConversationIndexKey key = new ConversationIndexKey(c.getBuyerId(), c.getSellerId(), c.getAdId());
            conversationIndexBucket.delete(key.serialize());
        }
        conversationBucket.delete(c.getId());
    }

    @Override
    public Optional<Conversation> findExistingConversationFor(ConversationIndexKey key) {
        Optional<String> conversationId = conversationIndexBucket.findConversationId(key);
        if (conversationId.isPresent()) {
            return Optional.fromNullable(getById(conversationId.get()));
        }
        return Optional.absent();
    }

}
