package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.Converter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.runtime.TimingReports;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 15:46
 *
 * @author maldana@ebay.de
 */
class PostBoxConverter implements Converter<PostBox> {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxConverter.class);

    private static final Histogram POSTBOX_BYTE_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.bytes");
    private static final Histogram POSTBOX_NUM_THREADS_SIZE_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sizes.num-conversations");
    private static final Histogram POSTBOX_NUM_UNREAD = TimingReports.newHistogram("riak-postbox-sizes.num-conversations-unread");

    private final PostBoxToJsonConverter toJson = new PostBoxToJsonConverter();
    private final JsonToPostBoxConverter toPostBox = new JsonToPostBoxConverter();
    private final String bucketName;
    private int maxConversationAgeDays;

    public PostBoxConverter(String bucketName, int maxConversationAgeDays) {
        this.bucketName = bucketName;
        this.maxConversationAgeDays = maxConversationAgeDays;
    }

    @Override
    public IRiakObject fromDomain(PostBox postBox, VClock vClock) {
        String json = toJson.toJson(postBox);

        byte[] compressed = GzipAwareContentFilter.compress(json);

        POSTBOX_BYTE_SIZE_HISTOGRAM.update(compressed.length);

        return RiakObjectBuilder.newBuilder(bucketName, postBox.getEmail())
                .withValue(compressed)
                .addIndex(PostBoxRepository.UPDATED_INDEX, DateTime.now().getMillis())
                .withVClock(vClock)
                .withContentType("application/x-gzip")
                .build();
    }

    @Override
    public PostBox toDomain(IRiakObject riakObject) {
        PostBox postBox = toPostBox.toPostBox(riakObject.getKey(), GzipAwareContentFilter.unpackIfGzipped(riakObject), maxConversationAgeDays);
        POSTBOX_NUM_THREADS_SIZE_HISTOGRAM.update(postBox.getConversationThreads().size());
        POSTBOX_NUM_UNREAD.update(postBox.getUnreadConversations().size());
        return postBox;
    }

}
