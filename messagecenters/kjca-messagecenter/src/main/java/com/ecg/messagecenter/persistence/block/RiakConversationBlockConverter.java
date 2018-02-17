package com.ecg.messagecenter.persistence.block;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.http.util.Constants;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class RiakConversationBlockConverter implements Converter<ConversationBlock> {
    @Autowired
    private ConversationBlockToJsonConverter toJson;

    @Autowired
    private JsonToConversationBlockConverter toConversationBlock;

    private String bucketName;

    @Autowired
    public void setBucketName(@Value("${persistence.riak.bucket.name.prefix:}") String bucketPrefix) {
        bucketName = bucketPrefix + RiakConversationBlockRepository.BUCKET_NAME;
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
          .addIndex(RiakConversationBlockRepository.CREATED_INDEX, DateTime.now().getMillis())
          .build();
    }

    @Override
    public ConversationBlock toDomain(IRiakObject riakObject) throws ConversionException {
        return toConversationBlock.toConversationBlock(riakObject.getKey(), riakObject.getValueAsString());
    }
}
