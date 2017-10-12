package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class RiakSimplePostBoxConverter implements Converter<PostBox> {
    private static final Histogram POSTBOX_BYTE_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.bytes");
    private static final Histogram POSTBOX_NUM_THREADS_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.num-conversations");
    private static final Histogram POSTBOX_NUM_UNREAD = TimingReports.newHistogram("riak-postbox-sizes.num-conversations-unread");

    @Autowired
    private AbstractPostBoxToJsonConverter toJson;

    @Autowired
    private AbstractJsonToPostBoxConverter toPostBox;

    @Value("${persistence.simple.bucket.name.prefix:}" + DefaultRiakSimplePostBoxRepository.POST_BOX)
    private String bucketName;

    @Override
    public IRiakObject fromDomain(PostBox postBox, VClock vClock) throws ConversionException {
        String json = toJson.toJson(postBox);

        byte[] compressed = RiakGzipAwareContentFilter.compress(json);

        POSTBOX_BYTE_SIZE_HISTOGRAM.update(compressed.length);

        return RiakObjectBuilder.newBuilder(bucketName, postBox.getEmail())
          .withValue(compressed)
          .addIndex(DefaultRiakSimplePostBoxRepository.UPDATED_INDEX, DateTime.now().getMillis())
          .withVClock(vClock)
          .withContentType("application/x-gzip")
          .build();
    }

    @Override
    public PostBox toDomain(IRiakObject riakObject) throws ConversionException {
        PostBox postBox = toPostBox.toPostBox(riakObject.getKey(), RiakGzipAwareContentFilter.unpackIfGzipped(riakObject));

        POSTBOX_NUM_THREADS_SIZE_HISTOGRAM.update(postBox.getConversationThreads().size());
        POSTBOX_NUM_UNREAD.update(postBox.getUnreadConversations().size());

        return postBox;
    }
}