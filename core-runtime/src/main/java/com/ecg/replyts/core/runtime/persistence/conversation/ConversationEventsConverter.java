package com.ecg.replyts.core.runtime.persistence.conversation;


import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.persistence.GZip;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;

import java.io.IOException;
import java.util.List;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.persistence.TimestampIndexValue.timestampInMinutes;
import static com.ecg.replyts.core.runtime.persistence.conversation.GzipAwareContentFilter.unpackIfGzipped;
import static java.lang.String.format;

class ConversationEventsConverter implements Converter<ConversationEvents> {


    private static final Histogram CONVERSATION_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-conversation-sizes.bytes");

    private final String bucketName;
    private final ConversationJsonSerializer conversationJsonSerializer;
    private final ValueSizeConstraint sizeConstraint;

    public ConversationEventsConverter(String bucketName, ConversationJsonSerializer conversationJsonSerializer) {
        this(bucketName, conversationJsonSerializer, ValueSizeConstraint.maxMb(30));
    }

    ConversationEventsConverter(String bucketName, ConversationJsonSerializer conversationJsonSerializer, ValueSizeConstraint sizeConstraint) {
        this.bucketName = bucketName;
        this.conversationJsonSerializer = conversationJsonSerializer;
        this.sizeConstraint = sizeConstraint;
    }


    @Override
    public IRiakObject fromDomain(ConversationEvents domainObject, VClock vclock) {

        List<ConversationEvent> events = domainObject.getEvents();
        if (events.isEmpty()) throw new ConversionException("Event list may not be empty");
        if (!(events.get(0) instanceof ConversationCreatedEvent)) {
            throw new ConversionException(format(
                    "First event must be a %s, it is a %s",
                    ConversationCreatedEvent.class.getName(), events.get(0).getClass().getName()));
        }

        ConversationCreatedEvent createdEvent = (ConversationCreatedEvent) events.get(0);
        String conversationId = createdEvent.getConversationId();


        Conversation conversation = ImmutableConversation.replay(events);

        byte[] binary;
        try {
            binary = conversationJsonSerializer.serialize(events);
            binary = GZip.zip(binary);
        } catch (IOException e) {
            throw new ConversionException(e);
        }

        if (sizeConstraint.isTooBig(binary.length)) {
            throw new IllegalArgumentException(format("Conversation to big. Size %d for conversation id %s is too big.", binary.length, conversation.getId()));
        }

        CONVERSATION_SIZE_HISTOGRAM.update(binary.length);

        int convSizeMb = binary.length / (1024 * 1024);
        newCounter("riak-conversation-sizes." + convSizeMb + "mb").inc();


        return RiakObjectBuilder.newBuilder(bucketName, conversationId).
                withValue(binary).
                withVClock(vclock).
                withContentType(GZip.GZIP_MIMETYPE).
                addIndex(ConversationBucket.SECONDARY_INDEX_MODIFIED_AT, timestampInMinutes(conversation.getLastModifiedAt())).
                addIndex(ConversationBucket.SECONDARY_INDEX_CREATED_AT, timestampInMinutes(conversation.getCreatedAt())).
                build();
    }

    @Override
    public ConversationEvents toDomain(IRiakObject riakObject) {

        byte[] json = unpackIfGzipped(riakObject);
        try {
            return new ConversationEvents(conversationJsonSerializer.deserialize(json));
        } catch (IOException e) {
            throw new ConversionException("could not parse json " + json, e);
        }
    }



}
