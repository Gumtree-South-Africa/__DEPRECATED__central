package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.http.util.Constants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ConversationBlockConverter implements Converter<ConversationBlock> {
    private final String bucketName;
    private final ConversationBlockToJsonConverter toJson = new ConversationBlockToJsonConverter();
    private final JsonToConversationBlockConverter toConversationBlock = new JsonToConversationBlockConverter();

    public ConversationBlockConverter(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public IRiakObject fromDomain(ConversationBlock conversationBlock, VClock vclock) throws ConversionException {
        String json = toJson.toJson(conversationBlock);

        return RiakObjectBuilder.newBuilder(bucketName, conversationBlock.getConversationId())
                .withValue(json)
                .withVClock(vclock)
                .withContentType(Constants.CTYPE_JSON_UTF8)
                // We could be smarter here and set the index to the conversation's start date, so that
                // the block gets expired at the same time. But that would cost us another lookup
                // and considering the tiny size of these block-state objects, it's not worth it
                .addIndex(ConversationBlockRepository.CREATED_INDEX, DateTime.now(DateTimeZone.UTC).getMillis())
                .build();
    }

    @Override
    public ConversationBlock toDomain(IRiakObject riakObject) throws ConversionException {
        return toConversationBlock.toConversationBlock(riakObject.getKey(), riakObject.getValueAsString());
    }
}
