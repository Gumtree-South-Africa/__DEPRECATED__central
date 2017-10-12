package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;

import static com.ecg.replyts.core.runtime.TimingReports.newGauge;

@Component
@ConditionalOnExpression("#{'${replyts2.cleanup.conversation.enabled}' == '${region}' && '${persistence.strategy}' == 'cassandra'}")
public class CassandraCleanupConversationCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraCleanupConversationCronJob.class);

    private static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired(required = false)
    @Qualifier("messageidSink")
    private KafkaSinkService msgidKafkaSink;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxAgeDays;

    @Value("${replyts.cleanup.conversation.streaming.queue.size:100}")
    private int workQueueSize;

    @Value("${replyts.cleanup.conversation.streaming.threadcount:4}")
    private int threadCount;

    @Value("${replyts.cleanup.conversation.streaming.batch.size:2000}")
    private int batchSize;

    @Value("${replyts.cleanup.conversation.schedule.expression:0 0 0 * * ? *}")
    private String cronJobExpression;

    private ThreadPoolExecutor threadPoolExecutor;

    @Value("${replyts.cleanup.conversation.rate.limit:1000}")
    private int conversationCleanupRateLimit;

    @Value("${replyts.cleanup.task.timeout.sec:60}")
    private int cleanupTaskTimeoutSec;

    private RateLimiter rateLimiter;

    private static final byte[] EMPTY = new byte[]{};

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(workQueueSize);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        this.rateLimiter = RateLimiter.create(conversationCleanupRateLimit);
    }

    @Override
    public void execute() throws Exception {
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(maxAgeDays, CLEANUP_CONVERSATION_JOB_NAME);

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date '{}'", cleanupDate);

        Stream<ConversationEventIdx> conversationEventIdxs = conversationRepository.streamConversationEventIdxsByHour(cleanupDate);

        List<Future<?>> cleanUpTasks = new ArrayList<>();

        Iterators.partition(conversationEventIdxs.iterator(), batchSize).forEachRemaining(idxs -> {
            cleanUpTasks.add(threadPoolExecutor.submit(() -> {

                Set<String> convIds = idxs.stream().collect(Collectors.toConcurrentMap(id -> id.getConversationId(), id -> true)).keySet();
                LOG.info("Cleanup: Deleting data related to {} de-duplicated conversation events out of {} events ", convIds.size(), idxs.size());

                convIds.forEach(conversationId -> {

                    double sleepTimeSeconds = rateLimiter.acquire();
                    newGauge("cleanup.conversations.rateLimiter.sleepTimeSecondsGauge", () -> sleepTimeSeconds);

                    Long lastModifiedDate = conversationRepository.getLastModifiedDate(conversationId);

                    // Round the lastModifiedDate to the day, then compare to the (already rounded) cleanup date

                    DateTime roundedLastModifiedDate = lastModifiedDate != null ? new DateTime((lastModifiedDate)).hourOfDay().roundFloorCopy().toDateTime() : null;

                    if (lastModifiedDate != null && (roundedLastModifiedDate.isBefore(cleanupDate) || roundedLastModifiedDate.equals(cleanupDate))) {
                        MutableConversation conversation = conversationRepository.getById(conversationId);
                        persistMessageId(conversation);
                        deleteConversationWithIdxs(conversation, conversationId);
                    }
                });
            }));
        });

        for (Future<?> task : cleanUpTasks) {
            try {
                task.get(cleanupTaskTimeoutSec, TimeUnit.SECONDS);
            } catch (ExecutionException | RuntimeException e) {
                LOG.error("Conversation cleanup task execution failure", e);
                return;
            } catch (InterruptedException e) {
                LOG.warn("The cleanup task has been interrupted");
                Thread.currentThread().interrupt();
                return;
            }
        }

        cronJobClockRepository.set(CLEANUP_CONVERSATION_JOB_NAME, now(), cleanupDate);

        LOG.info("Cleanup: Finished deleting conversations.");
    }

    private void persistMessageId(Conversation conversation) {
        if (msgidKafkaSink == null || conversation == null) {
            return;
        }
        try {
            for (Message msg : conversation.getMessages()) {

                for(String name : msg.getAttachmentFilenames()) {
                    String key = attachmentRepository.getCompositeKey(msg.getId(), name);
                    msgidKafkaSink.store(key, EMPTY);
                }

            }
        } catch (RuntimeException re) {
            LOG.error("Cleanup: Failed to save messageId into Kafka due to {} ", re.getMessage(), re);
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return cronJobExpression;
    }

    private void deleteConversationWithIdxs(MutableConversation conversation, String conversationId) {
        try {
            if (conversation != null) {
                conversation.applyCommand(new ConversationDeletedCommand(conversation.getId(), now()));
                ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
            } else {
                conversationRepository.deleteConversationModificationIdxs(conversationId);
            }
        } catch (RuntimeException ex) {
            LOG.error("Cleanup: Could not delete Conversation: " + conversationId, ex);
        }
    }

}