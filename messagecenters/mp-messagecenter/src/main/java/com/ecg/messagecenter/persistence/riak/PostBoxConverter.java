package com.ecg.messagecenter.persistence.riak;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.codahale.metrics.Histogram;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;

class PostBoxConverter implements Converter<PostBox> {

    private static final Histogram POSTBOX_BYTE_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.bytes");
    private static final Histogram POSTBOX_NUM_THREADS_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.num-conversations");
    private static final Histogram POSTBOX_NUM_UNREAD = TimingReports.newHistogram("riak-postbox-sizes.num-conversations-unread");

    private final PostBoxToJsonConverter toJson = new PostBoxToJsonConverter();
    private final JsonToPostBoxConverter toPostBox = new JsonToPostBoxConverter();

    @Override
    public IRiakObject fromDomain(PostBox postBox, VClock vClock) throws ConversionException {
        String json = toJson.toJson(postBox);

        byte[] compressed = GzipAwareContentFilter.compress(json);

        POSTBOX_BYTE_SIZE_HISTOGRAM.update(compressed.length);

        return RiakObjectBuilder.newBuilder(DefaultRiakPostBoxRepository.POST_BOX, postBox.getUserId())
                .withValue(compressed)
                .addIndex(DefaultRiakPostBoxRepository.UPDATED_INDEX, DateTime.now().getMillis())
                .withVClock(vClock)
                .withContentType("application/x-gzip")
                .build();
    }

    @Override
    public PostBox toDomain(IRiakObject riakObject) throws ConversionException {
        PostBox postBox = toPostBox.toPostBox(riakObject.getKey(), GzipAwareContentFilter.unpackIfGzipped(riakObject));
        POSTBOX_NUM_THREADS_SIZE_HISTOGRAM.update(postBox.getConversationThreads().size());
        POSTBOX_NUM_UNREAD.update(postBox.getUnreadConversations().size());
        return postBox;
    }
}