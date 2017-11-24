package com.ecg.comaas.r2cmigration.difftool.repo;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.DiffToolConfiguration;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockConflictResolver;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockConverter;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class RiakConversationBlockRepo {

    private static final Logger LOG = LoggerFactory.getLogger(RiakConversationBlockRepo.class);

    private static final Timer GET_BY_ID_RIAK_TIMER = TimingReports.newTimer("riak-getById");

    @Autowired
    private RiakConversationBlockConverter riakConversationBlockConverter;

    private final RiakConversationBlockConflictResolver riakConversationBlockConflictResolver;

    private IRiakClient riakClient;
    private String bucketNamePrefix = "";

    @Autowired
    public RiakConversationBlockRepo(IRiakClient riakClient, @Value("${persistence.riak.bucket.name.prefix:}") String bucketNamePrefix) {
        this.riakClient = riakClient;

        if (StringUtils.isNotBlank(bucketNamePrefix)) {
            LOG.info("Using riak bucket prefix: {}", bucketNamePrefix.trim());
            this.bucketNamePrefix = bucketNamePrefix.trim();
        } else {
            LOG.info("No riak bucket prefix configured");
        }

        this.riakConversationBlockConflictResolver = new RiakConversationBlockConflictResolver();
    }

    private Bucket getBucket(String bucketName) throws RiakRetryFailedException {
        LOG.debug("Fetching riak bucket {}", bucketName);
        return riakClient.fetchBucket(bucketName).execute();
    }

    public Bucket getConversationBlockBucket() throws RiakRetryFailedException {
        return getBucket(bucketNamePrefix + DiffToolConfiguration.RIAK_CONVERSATION_BLOCK_BUCKET_NAME);
    }

    public ConversationBlock fetchConversationBlock(String convId, Bucket bucket) throws RiakRetryFailedException {
        LOG.info("Fetching conversationBlock from Riak with id {} and bucket {}", convId, bucket.getName());
        try (Timer.Context ignored = GET_BY_ID_RIAK_TIMER.time()) {
            return bucket.fetch(convId, ConversationBlock.class).
                    withConverter(riakConversationBlockConverter).
                    withResolver(riakConversationBlockConflictResolver).
                    withRetrier(new DefaultRetrier(3)).
                    notFoundOK(false).
                    execute();
        }
    }

    public StreamingOperation<String> streamConvBlockIds(Bucket bucket) {
        try {
            return bucket.keys();
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }
    }
}
