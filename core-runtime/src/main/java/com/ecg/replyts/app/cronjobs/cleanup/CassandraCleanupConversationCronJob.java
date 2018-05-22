package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.CleanupConfiguration;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.ConversationDeletedCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventId;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.model.conversation.InvalidConversationException;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraHeldMailRepository;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.ecg.replyts.core.runtime.TimingReports.newGauge;
import static org.joda.time.DateTime.now;

@Component
@ConditionalOnProperty(name = "CRONJOBS_ENABLED", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("#{'${cronjob.cleanup.conversation.enabled:false}' == 'true' && '${active.dc}' != '${region}'}")
public class CassandraCleanupConversationCronJob implements CronJobExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraCleanupConversationCronJob.class);

    private static final String CLEANUP_CONVERSATION_JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private CassandraHeldMailRepository cassandraHeldMailRepository;

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

    @Autowired
    private CleanupConfiguration config;

    private ExecutorService threadPoolExecutor;

    private RateLimiter rateLimiter;

    private static final byte[] EMPTY = new byte[]{};

    @PostConstruct
    public void createThreadPoolExecutor() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(config.getWorkQueueSize());
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("cleanup", CassandraCleanupConversationCronJob.class.getSimpleName());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(config.getThreadCount(), config.getThreadCount(), 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        this.threadPoolExecutor = new InstrumentedExecutorService(executor, "cleanup", CassandraCleanupConversationCronJob.class.getSimpleName());

        this.rateLimiter = RateLimiter.create(config.getConversationCleanupRateLimit());
        LOG.info("messageidSink is enabled {}", msgidKafkaSink != null);
    }

    @Override
    public void execute() throws Exception {
        LOG.info("Started Cleanup Conversation Cronjob");
        DateTime cleanupDate = cleanupDateCalculator.getCleanupDate(config.getMaxConversationAgeDays(), CLEANUP_CONVERSATION_JOB_NAME);

        if (cleanupDate == null) {
            return;
        }

        LOG.info("Cleanup: Deleting conversations for the date '{}'", cleanupDate);

        Stream<? extends ConversationEventId> conversationEventIdxs = conversationRepository.streamConversationEventIndexesByHour(cleanupDate);

        List<Future<?>> cleanUpTasks = createCleanUpTasks(conversationEventIdxs, cleanupDate);

        waitCleanUpTasksAndLog(cleanUpTasks, cleanupDate);

        LOG.info("Finished Cleanup Conversation Cronjob");
    }

    private void waitCleanUpTasksAndLog(List<Future<?>> cleanUpTasks, DateTime cleanupDate) throws java.util.concurrent.TimeoutException {
        for (Future<?> task : cleanUpTasks) {
            try {
                task.get(config.getCleanupTaskTimeoutSec(), TimeUnit.SECONDS);
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
    }

    private List<Future<?>> createCleanUpTasks(Stream<? extends ConversationEventId> conversationEventIdxs, DateTime cleanupDate) {
        List<Future<?>> cleanUpTasks = new ArrayList<>();
        Iterators.partition(conversationEventIdxs.iterator(), config.getBatchSize()).forEachRemaining(idxs -> cleanUpTasks.add(threadPoolExecutor.submit(() -> {

            MDCConstants.setTaskFields(CassandraCleanupConversationCronJob.class.getSimpleName());

            processConversationIds(cleanupDate, idxs);
        })));

        return cleanUpTasks;
    }

    private void processConversationIds(DateTime cleanupDate, List<? extends ConversationEventId> idxs) {
        LOG.info("Cleanup: Deleting data related to {} conversation events", idxs.size());

        idxs.forEach(conversationIdx -> {
            String conversationId = conversationIdx.getConversationId();
            double sleepTimeSeconds = rateLimiter.acquire();
            newGauge("cleanup.conversations.rateLimiter.sleepTimeSecondsGauge", () -> sleepTimeSeconds);

            Long lastModifiedDate = conversationRepository.getLastModifiedDate(conversationId);

            // Round the lastModifiedDate to the day, then compare to the (already rounded) cleanup date
            DateTime roundedLastModifiedDate = lastModifiedDate != null ? new DateTime((lastModifiedDate)).hourOfDay().roundFloorCopy().toDateTime() : null;

            if (lastModifiedDate != null && (roundedLastModifiedDate.isBefore(cleanupDate) || roundedLastModifiedDate.equals(cleanupDate))) {
                deleteConversationById(conversationId);
            }
        });
    }

    private void deleteConversationById(String conversationId) {
        try {
            MutableConversation conversation = conversationRepository.getById(conversationId);
            persistMessageId(conversation);
            deleteConversationWithIdxs(conversation, conversationId);
        } catch (InvalidConversationException ie) {
            LOG.warn("Cannot load conversation {} - due to lack of ConversationCreatedEvent, please remove manually." +
                    " Existing events are {}", conversationId, ie.getEvents());
            deleteConversationWithIdxs(null, conversationId);
        }
    }

    private void deleteHeldMessagesFrom(MutableConversation conversation) {
        for (Message message : conversation.getMessages()) {
            cassandraHeldMailRepository.remove(message.getId());
        }
    }

    private void persistMessageId(Conversation conversation) {
        if (msgidKafkaSink == null || conversation == null) {
            return;
        }
        try {
            for (Message msg : conversation.getMessages()) {

                for (String name : msg.getAttachmentFilenames()) {
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
        return config.getCronJobExpression();
    }

    private void deleteConversationWithIdxs(MutableConversation conversation, String conversationId) {
        try {
            if (conversation != null) {
                conversation.applyCommand(new ConversationDeletedCommand(conversation.getId(), now()));
                ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
                deleteHeldMessagesFrom(conversation);
            } else {
                conversationRepository.deleteConversationModificationIdxs(conversationId);
            }
        } catch (RuntimeException ex) {
            LOG.error("Cleanup: Could not delete Conversation: " + conversationId, ex);
        }
    }
}