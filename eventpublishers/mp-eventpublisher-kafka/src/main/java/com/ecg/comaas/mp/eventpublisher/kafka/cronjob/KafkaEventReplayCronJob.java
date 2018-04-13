package com.ecg.comaas.mp.eventpublisher.kafka.cronjob;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.cron.CronJobExecutor;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.app.eventpublisher.EventConverter;
import com.ecg.replyts.app.eventpublisher.EventPublisher;
import com.google.common.collect.Iterators;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class KafkaEventReplayCronJob implements CronJobExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventReplayCronJob.class);

    private static final Timer TIMER = TimingReports.newTimer("kafkaEventReplayCronJob-replayEvents");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

    private final String cronExpression;

    private final ConversationRepository conversationRepository;
    private final String startDateString;
    private final String endDateString;

    private final int eventBatchSize;
    private final ThreadPoolExecutor threadPoolExecutor;

    private final EventPublisher eventPublisher;
    private final EventConverter eventConverter;

    KafkaEventReplayCronJob(
            String cronExpression,
            ConversationRepository conversationRepository,
            String startDateString, String endDateString,
            int eventBatchSize, int streamingThreadCount, int workQueueSize,
            EventPublisher eventPublisher, MailCloakingService mailCloakingService) {
        this.cronExpression = cronExpression;

        this.conversationRepository = conversationRepository;
        this.startDateString = startDateString;
        this.endDateString = endDateString;

        this.eventBatchSize = eventBatchSize;
        this.threadPoolExecutor = new ThreadPoolExecutor(
                streamingThreadCount,
                streamingThreadCount,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(workQueueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());

        this.eventPublisher = eventPublisher;
        this.eventConverter = new EventConverter(mailCloakingService);
    }

    @Override
    public void execute() throws Exception {
        try (Timer.Context ignored = TIMER.time()) {

            LOGGER.info("Started replaying events to Kafka.");

            DateTime startDateTime = DATE_TIME_FORMATTER.parseDateTime(startDateString);
            DateTime endDateTime = DATE_TIME_FORMATTER.parseDateTime(endDateString);
            Stream<ImmutablePair<Conversation, ConversationEvent>> stream;

            if (conversationRepository instanceof CassandraConversationRepository) {
                stream = ((CassandraConversationRepository) conversationRepository).findEventsCreatedBetween(startDateTime, endDateTime);
            } else {
                throw new IllegalStateException("Conversation repository is not a Cassandra repository");
            }

            List<Future<?>> eventReplayTasks = new ArrayList<>();

            Iterators.partition(stream.iterator(), eventBatchSize).forEachRemaining(pairs ->
                    eventReplayTasks.add(threadPoolExecutor.submit(() -> {
                        LOGGER.info("Replaying {} events to Kafka", pairs.size());
                        eventPublisher.publishConversationEvents(
                                eventConverter.toEvents(pairs));
                    })));

            eventReplayTasks.forEach(task -> {
                try {
                    task.get();
                } catch (CancellationException | ExecutionException ex) {
                    LOGGER.warn("Failed to save events to Kafka due to {} ", ex.getMessage(), ex);
                } catch (InterruptedException e) {
                    LOGGER.warn("Interrupted while waiting for a task to complete");
                    Thread.currentThread().interrupt();
                }
            });

            LOGGER.info("Finished replaying events to Kafka.");
        }
    }

    @Override
    public String getPreferredCronExpression() {
        return cronExpression;
    }
}
