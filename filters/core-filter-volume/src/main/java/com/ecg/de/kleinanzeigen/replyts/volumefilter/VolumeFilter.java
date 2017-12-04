package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.de.kleinanzeigen.replyts.volumefilter.registry.OccurrenceRegistry;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

class VolumeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);

    private static final String ESPER_ALREADY_NOTIFIED = "ESPER_ALREADY_NOTIFIED";
    private static final Comparator<Window> ORDERED_BY_QUOTA_TIME_SPAN =
            Comparator.comparing(Window::getQuota, Quota.TIME_SPAN_COMPARATOR);

    private final SharedBrain sharedBrain;
    private final Collection<Window> windows;
    private final EventStreamProcessor processor;
    private final Duration cassandraImplementationTimeout;
    private final boolean cassandraImplementationEnabled;
    private final OccurrenceRegistry occurrenceRegistry;
    private final Executor cassandraImplementationExecutor;

    VolumeFilter(SharedBrain sharedBrain, EventStreamProcessor processor, Collection<Window> windows,
                 Duration cassandraImplementationTimeout, boolean cassandraImplementationEnabled,
                 @Nullable OccurrenceRegistry occurrenceRegistry, int cassandraImplementationThreads) {
        this.sharedBrain = sharedBrain;
        this.processor = processor;
        this.windows = Ordering.from(ORDERED_BY_QUOTA_TIME_SPAN).immutableSortedCopy(windows);
        this.cassandraImplementationTimeout = cassandraImplementationTimeout;
        this.cassandraImplementationEnabled = cassandraImplementationEnabled;
        this.occurrenceRegistry = occurrenceRegistry;
        this.cassandraImplementationExecutor = Executors.newFixedThreadPool(cassandraImplementationThreads);
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        CompletableFuture<FilterFeedback> future = filterWithcassandraImplementationAsync(context);

        String senderMail = extractSender(context);

        // Only the first VolumeFilter should notify the other Esper/Hazelcast nodes.
        Map<String, Object> filterContext = context.getFilterContext();
        if (!filterContext.containsKey(ESPER_ALREADY_NOTIFIED)) {
            sharedBrain.markSeen(senderMail);
            filterContext.put(ESPER_ALREADY_NOTIFIED, Boolean.TRUE);
        }

        for (Window window : windows) {
            long mailsInWindow = processor.count(senderMail, window);
            Quota quota = window.getQuota();

            LOG.trace("Num of mails in {} {}: {}", quota.getPerTimeValue(), quota.getPerTimeUnit(), mailsInWindow);

            if (mailsInWindow > quota.getAllowance()) {
                FilterFeedback feedback = quotaExceededFeedback(mailsInWindow, quota);
                compareFeedbacks(feedback, future);
                return Collections.singletonList(feedback);
            }
        }
        compareFeedbacks(null, future);
        return Collections.emptyList();
    }

    private static FilterFeedback quotaExceededFeedback(long mailsInWindow, Quota quota) {
        return new FilterFeedback(
                quota.uiHint(),
                quota.describeViolation(mailsInWindow),
                quota.getScore(),
                FilterResultState.OK);
    }

    @VisibleForTesting
    String extractSender(MessageProcessingContext context) {
        Message message = context.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        return context.getConversation().getUserId(fromRole);
    }

    //
    // ============================== cut here ==============================
    //

    @Nullable
    private FilterFeedback filterWithcassandraImplementation(MessageProcessingContext context) {
        String sender = extractSender(context);
        Date now = new Date();
        occurrenceRegistry.register(sender, context.getMessageId(), context.getMessage().getReceivedAt().toDate());
        for (Window window : windows) {
            Quota quota = window.getQuota();
            Date fromTime = Date.from(now.toInstant().minusMillis(quota.getTimeSpanMillis()));
            long mailsInWindow = occurrenceRegistry.count(sender, fromTime);

            LOG.trace("[cassandra implementation] Num of mails in {} {}: {}", quota.getPerTimeValue(), quota.getPerTimeUnit(), mailsInWindow);
            if (mailsInWindow > quota.getAllowance()) {
                return quotaExceededFeedback(mailsInWindow, quota);
            }
        }
        return null;
    }

    //
    // ============================== cut here ==============================
    //

    private CompletableFuture<FilterFeedback> filterWithcassandraImplementationAsync(MessageProcessingContext context) {
        if (!cassandraImplementationEnabled) {
            return CompletableFuture.supplyAsync(() -> null, MoreExecutors.directExecutor());
        }
        return CompletableFuture.supplyAsync(() -> filterWithcassandraImplementation(context), cassandraImplementationExecutor);
    }

    private static final Counter FAILED_COUNTER = TimingReports.newCounter("experimental.volumeFilter.failed");
    private static final Counter TIMEOUT_COUNTER = TimingReports.newCounter("experimental.volumeFilter.timeout");
    private static final Counter EQ_COUNTER = TimingReports.newCounter("experimental.volumeFilter.eq");
    private static final Counter NEQ_COUNTER = TimingReports.newCounter("experimental.volumeFilter.neq");
    private static final Timer CASSANDRA_FILTER_TIMER = TimingReports.newTimer("experimental.volumeFilter.time");

    private void compareFeedbacks(@Nullable FilterFeedback oldApproachFeedback,
                                  @Nonnull CompletableFuture<FilterFeedback> possiblyNewFeedback) {
        if (!cassandraImplementationEnabled) {
            return;
        }
        try {
            FilterFeedback cassandraImplementationFeedback = waitForcassandraImplementationFeedback(possiblyNewFeedback).orElse(null);
            if (Objects.equals(cassandraImplementationFeedback, oldApproachFeedback)) {
                LOG.trace("feedbacks match");
                EQ_COUNTER.inc();
            } else {
                LOG.trace("feedbacks DOESN'T match");
                NEQ_COUNTER.inc();
            }
        } catch (Exception e) { // we ain't need no surprises
            LOG.trace("feedback comparison has failed due to an unexpected reason", e);
            FAILED_COUNTER.inc();
        }
    }

    private Optional<FilterFeedback> waitForcassandraImplementationFeedback(CompletableFuture<FilterFeedback> future) {
        try (Timer.Context ignore = CASSANDRA_FILTER_TIMER.time()) {
            return Optional.ofNullable(future.get(cassandraImplementationTimeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            LOG.trace("the cassandra implementation has been interrupted");
            Thread.currentThread().interrupt();
            FAILED_COUNTER.inc();
        } catch (ExecutionException e) {
            LOG.trace("the cassandra implementation has failed", e.getCause());
            FAILED_COUNTER.inc();
        } catch (TimeoutException e) {
            LOG.trace("the cassandra implementation has timed out");
            TIMEOUT_COUNTER.inc();
        }
        return Optional.empty();
    }
}
