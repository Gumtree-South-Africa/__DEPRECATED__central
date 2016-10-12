package com.ecg.messagecenter.migration;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.simple.HybridSimplePostBoxRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class PostboxMigrationChunkHandler {

    private static final Timer OVERALL_TIMER = TimingReports.newTimer("migrate-chunk-postbox");

    private final HybridSimplePostBoxRepository postboxRepository;
    private final int maxChunkSize;

    private static final Logger LOG = LoggerFactory.getLogger(PostboxMigrationChunkHandler.class);
    private static final Logger FAILED_POSTBOX_IDS = LoggerFactory.getLogger("FailedToFetchPostboxes");

    PostboxMigrationChunkHandler(HybridSimplePostBoxRepository postboxRepository, int maxChunkSize) {
        this.postboxRepository = postboxRepository;
        this.maxChunkSize = maxChunkSize;
    }

    public void migrateChunk(List<String> postboxIds) {
        if (postboxIds.size() > maxChunkSize) {
            LOG.info("Partitioning postbox list with {} elements into chunks of size {}", postboxIds.size(), maxChunkSize);
        }
        List<List<String>> partitions = Lists.partition(postboxIds, maxChunkSize);

        for (List<String> partition : partitions) {
            migrateChunkPartition(partition);
        }
    }

    private void migrateChunkPartition(List<String> postboxIds) {
        if (postboxIds.isEmpty()) {
            return;
        }
        try (Timer.Context ignored = OVERALL_TIMER.time()) {
            fetchPostboxes(postboxIds);
        }
    }

    public void fetchPostboxes(List<String> postboxIds) {
        int fetchedPostboxCounter = 0;
        for (String postboxId : postboxIds) {
            try {
                PostBox postbox = postboxRepository.byId(postboxId);
                // might be null for very old postbox that have been removed by the cleanup job while the indexer
                // was running.
                if (postbox != null) {
                    fetchedPostboxCounter++;
                }
            } catch (Exception e) {
                LOG.error(String.format("Migrator could not load postbox {} from repository - skipping it", postboxId), e);
                FAILED_POSTBOX_IDS.info(postboxId);
            }
        }
        if(fetchedPostboxCounter > 0) {

            LOG.debug("Fetch {} postboxes", fetchedPostboxCounter);
        }
        if(fetchedPostboxCounter != postboxIds.size() ) {

            LOG.warn("At least some postbox IDs were not found in the database, {} postboxes expected, but only {} retrieved",
                    postboxIds.size(), fetchedPostboxCounter);
        }
    }



}