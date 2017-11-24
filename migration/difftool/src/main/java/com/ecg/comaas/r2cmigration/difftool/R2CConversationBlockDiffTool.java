package com.ecg.comaas.r2cmigration.difftool;

import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.query.StreamingOperation;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.comaas.r2cmigration.difftool.repo.CassConversationBlockRepo;
import com.ecg.comaas.r2cmigration.difftool.repo.RiakConversationBlockRepo;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

public class R2CConversationBlockDiffTool {

    private static final Logger LOG = LoggerFactory.getLogger(R2CConversationBlockDiffTool.class);

    private final static Timer RIAK_TO_CASS_BATCH_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.batch-compare-timer");
    private final static Timer RIAK_TO_CASS_COMPARE_TIMER = TimingReports.newTimer("riak-to-cass.compare-timer");

    final static Counter RIAK_TO_CASS_CONV_MISMATCH_COUNTER = TimingReports.newCounter("riak-conversation-mismatch-counter");

    private int idBatchSize;

    Counter riakConversationBlockCounter;
    Counter cassConversationBlockCounter;

    volatile boolean isRiakMatchesCassandra = true;

    @Autowired
    private RiakConversationBlockRepo riakConversationBlockRepo;

    @Autowired
    private CassConversationBlockRepo cassConversationBlockRepo;

    @Autowired
    private ExecutorService executor;

    public R2CConversationBlockDiffTool(int idBatchSize) {
        this.idBatchSize = idBatchSize;
        this.riakConversationBlockCounter = newCounter("riakConversationBlockCounter");
        this.cassConversationBlockCounter = newCounter("cassConversationBlockCounter");
    }

    void compareRiakToCassandra(String... conversationIds) throws RiakException {
        compareRiakToCassAsync(
                Arrays.stream(conversationIds).collect(Collectors.toList()),
                riakConversationBlockRepo.getConversationBlockBucket()
        );
    }

    private void compareRiakToCassAsync(List<String> conversationIdsFromRiak, Bucket convBucket) {
        try (Timer.Context ignored = RIAK_TO_CASS_BATCH_COMPARE_TIMER.time()) {

            for (String conversationId : conversationIdsFromRiak) {
                LOG.debug("Next riak conversationId {}", conversationId);

                try (Timer.Context ignore = RIAK_TO_CASS_COMPARE_TIMER.time()) {
                    ConversationBlock riakConvBlock = riakConversationBlockRepo.fetchConversationBlock(conversationId, convBucket);
                    ConversationBlock cassConvBlock = cassConversationBlockRepo.getById(conversationId);

                    riakConversationBlockCounter.inc();
                    if (cassConvBlock != null) {
                        cassConversationBlockCounter.inc();
                    }

                    boolean equals = riakConvBlock.equals(cassConvBlock);
                    if (equals) {
                        LOG.debug("ConversationBlock with id {} SAME", conversationId);
                    } else {
                        RIAK_TO_CASS_CONV_MISMATCH_COUNTER.inc();
                        isRiakMatchesCassandra = false;
                        LOG.warn("NOT SAME \n riakConvBlock:{} \n cassConvBlock:{}", riakConvBlock, cassConvBlock);
                    }
                } catch (NullPointerException npe) {
                    LOG.warn("Riak to Cassandra comparison failed for id {}", conversationId, npe);
                }
            }
        } catch (RiakRetryFailedException e) {
            LOG.error("Fetching conversation from riak fails with ", e);
        }
    }

    List<Future> compareRiakToCassandra() throws RiakException {
        List<Future> results = new ArrayList<>(idBatchSize);
        Bucket convBlockBucket = riakConversationBlockRepo.getConversationBlockBucket();
        LOG.info("conversation_block bucket {}", convBlockBucket);
        StreamingOperation<String> convIdStream = riakConversationBlockRepo.streamConvBlockIds(convBlockBucket);
        Iterators.partition(convIdStream.iterator(), idBatchSize)
                .forEachRemaining(convIdIdx -> results.add(executor.submit(() -> compareRiakToCassAsync(convIdIdx, convBlockBucket))));
        return results;
    }
}
