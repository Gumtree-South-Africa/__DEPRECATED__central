package com.ecg.messagecenter.migration;

import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.runtime.indexer.IndexingMode;
import com.google.common.base.Stopwatch;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ConversationBlockMigration {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationBlockMigration.class);

    private final HazelcastInstance hazelcast;
    private final ConversationBlockRepository conversationBlockRepository;
    private final ThreadPoolExecutor executor;

    private Stopwatch watch;
    private AtomicLong totalProcessed = new AtomicLong();
    private ProcessingStatus processingStatus = ProcessingStatus.IDLE;

    public ConversationBlockMigration(HazelcastInstance hazelcast, ConversationBlockRepository conversationBlockRepository, ThreadPoolExecutor executor) {
        this.hazelcast = hazelcast;
        this.conversationBlockRepository = conversationBlockRepository;
        this.executor = executor;
    }

    public boolean migrateAllConversationBlocks() {
        return execute(this::callAllConvBlocksById);
    }

    private void callAllConvBlocksById() {
        LOG.info("Full conversation migration started at {}", new LocalDateTime());
        try {
            watch = Stopwatch.createStarted();
            totalProcessed.set(0);
            processingStatus = ProcessingStatus.PROCESSING;

            conversationBlockRepository.getIds();
            for (String conversationId : conversationBlockRepository.getIds()) {
                conversationBlockRepository.byId(conversationId);
                totalProcessed.addAndGet(1);
            }

            watch.stop();
            processingStatus = ProcessingStatus.COMPLETED;
        } finally {
            hazelcast.getLock(IndexingMode.MIGRATION.toString()).forceUnlock(); // have to use force variant as current thread is not the owner of the lock
        }
        LOG.info("Full conversation migration completed at {}", new LocalDateTime());
    }

    private boolean execute(Runnable runnable) {
        ILock lock = hazelcast.getLock(IndexingMode.MIGRATION.toString());
        try {
            if (lock.tryLock()) {
                executor.execute(runnable);
                LOG.info("Executing conversationBlock migration");
                return true;
            } else {
                LOG.info("Skipped execution conversationBlock migration due to no lock");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exception while waiting on a lock", e);
            lock.unlock();
            executor.getQueue().clear();
            throw new RuntimeException(e);
        }
    }

    public long getTimeTaken(TimeUnit timeUnit) {
        return watch.elapsed(timeUnit);
    }

    public long getMigratedCount() {
        return totalProcessed.get();
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    enum ProcessingStatus {
        IDLE, PROCESSING, COMPLETED
    }
}
