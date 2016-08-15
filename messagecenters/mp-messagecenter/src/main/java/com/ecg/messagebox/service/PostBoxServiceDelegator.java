package com.ecg.messagebox.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.configuration.DiffConfiguration;
import com.ecg.messagebox.configuration.NewModelConfiguration;
import com.ecg.messagebox.converters.ConversationResponseConverter;
import com.ecg.messagebox.converters.PostBoxResponseConverter;
import com.ecg.messagebox.converters.UnreadCountsConverter;
import com.ecg.messagebox.diff.Diff;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.util.InstrumentedCallerRunsPolicy;
import com.ecg.messagebox.util.InstrumentedExecutorService;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.MetricsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static java.lang.Runtime.getRuntime;

@Component("postBoxServiceDelegator")
public class PostBoxServiceDelegator implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxServiceDelegator.class);
    private static final Logger DIFF_LOGGER = LoggerFactory.getLogger("diffLogger");

    private final Timer processNewMessageTimer = newTimer("postBoxDelegatorService.processNewMessage");
    private final Timer getConversationTimer = newTimer("postBoxDelegatorService.getConversation");
    private final Timer markConversationAsReadTimer = newTimer("postBoxDelegatorService.markConversationAsRead");
    private final Timer getConversationsTimer = newTimer("postBoxDelegatorService.getConversations");
    private final Timer deleteConversationsTimer = newTimer("postBoxDelegatorService.deleteConversations");
    private final Timer getUnreadCountsTimer = newTimer("postBoxDelegatorService.getUnreadCounts");
    private final Timer getResponseDataTimer = newTimer("postBoxDelegatorService.getResponseData");

    private final Counter oldModelFailureCounter = newCounter("postBoxDelegatorService.oldModel.failed");
    private final Counter newModelFailureCounter = newCounter("postBoxDelegatorService.newModel.failed");
    private final Counter diffFailureCounter = newCounter("postBoxDelegatorService.diff.failed");

    private final PostBoxService oldPostBoxService;
    private final com.ecg.messagebox.service.PostBoxService newPostBoxService;

    // use separate execution services for the old model, new model and diff tool calls
    private final ExecutorService execSrvForOldModel;
    private final ExecutorService execSrvForNewModel;
    private final ExecutorService execSrvForDiff;

    private final boolean oldModelEnabled;
    private final NewModelConfiguration newModelConfig;

    private final ConversationResponseConverter conversationResponseConverter;
    private final PostBoxResponseConverter postBoxResponseConverter;
    private final UnreadCountsConverter unreadCountsConverter;

    private final DiffConfiguration diffConfig;
    private final Diff diff;

    // the old model is restricted to a hard limit of 500 total messages per conversation
    // (see ProcessingFinalizer#MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
    // so keeping it until we migrate to the new data model
    private final int messagesLimit;

    @Autowired
    public PostBoxServiceDelegator(@Qualifier("oldCassandraPostBoxService") PostBoxService oldPostBoxService,
                                   @Qualifier("newCassandraPostBoxService") com.ecg.messagebox.service.PostBoxService newPostBoxService,
                                   PostBoxResponseConverter postBoxResponseConverter,
                                   ConversationResponseConverter conversationResponseConverter,
                                   UnreadCountsConverter unreadCountsConverter,
                                   Diff diff,
                                   NewModelConfiguration newModelConfig,
                                   DiffConfiguration diffConfig,
                                   @Value("${messagebox.oldModel.enabled:true}") boolean oldModelEnabled,
                                   @Value("${messagebox.messagesLimit:500}") int messagesLimit) {

        this.oldPostBoxService = oldPostBoxService;
        this.newPostBoxService = newPostBoxService;

        this.oldModelEnabled = oldModelEnabled;
        this.newModelConfig = newModelConfig;

        this.conversationResponseConverter = conversationResponseConverter;
        this.postBoxResponseConverter = postBoxResponseConverter;
        this.unreadCountsConverter = unreadCountsConverter;

        this.messagesLimit = messagesLimit;

        this.diffConfig = diffConfig;
        this.diff = diff;

        execSrvForOldModel = newExecutorService("oldModelExecSrv");
        execSrvForNewModel = newExecutorService("newModelExecSrv");
        execSrvForDiff = newExecutorServiceForDiff();

        logMetrics();
    }

    @Override
    public void processNewMessage(String userId, Conversation rtsConversation, Message rtsMessage,
                                  ConversationRole conversationRole, boolean newReplyArrived) {
        try (Timer.Context ignored = processNewMessageTimer.time()) {

            CompletableFuture newModelFuture = runAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> newPostBoxService.processNewMessage(userId, rtsConversation, rtsMessage, newReplyArrived),
                    execSrvForNewModel,
                    String.format("NewModel processNewMessage - Could not process new message for userId %s, conversationId %s and messageId %s",
                            userId, rtsConversation.getId(), rtsMessage.getId()),
                    newModelFailureCounter
            );

            CompletableFuture oldModelFuture = runAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.processNewMessage(userId, rtsConversation, rtsMessage, conversationRole, newReplyArrived),
                    execSrvForOldModel,
                    String.format("OldModel processNewMessage - Could not process new message for userId %s, conversationId %s and messageId %s",
                            userId, rtsConversation.getId(), rtsMessage.getId()),
                    oldModelFailureCounter
            );

            (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String userId, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            CompletableFuture<Optional<ConversationResponse>> newModelFuture = supplyAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> newPostBoxService.getConversation(userId, conversationId, Optional.empty(), messagesLimit)
                            .map(conv -> conversationResponseConverter.toConversationResponse(conv, userId)),
                    execSrvForNewModel,
                    String.format("NewModel getConversation - Could not get conversation for userId %s and conversationId %s",
                            userId, conversationId),
                    newModelFailureCounter
            );

            CompletableFuture<Optional<ConversationResponse>> oldModelFuture = supplyAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.getConversation(userId, conversationId),
                    execSrvForOldModel,
                    String.format("OldModel getConversation - Could not get conversation for userId %s and conversationId %s",
                            userId, conversationId),
                    oldModelFailureCounter
            );

            doDiff(diffConfig.useDiff(userId),
                    () -> diff.conversationResponseDiff(userId, conversationId, newModelFuture, oldModelFuture),
                    "Error diff-ing conversation responses");

            return (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public Optional<ConversationResponse> markConversationAsRead(String userId, String conversationId) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            CompletableFuture<Optional<ConversationResponse>> newModelFuture = supplyAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> newPostBoxService.markConversationAsRead(userId, conversationId, Optional.empty(), messagesLimit)
                            .map(conv -> conversationResponseConverter.toConversationResponse(conv, userId)),
                    execSrvForNewModel,
                    String.format("NewModel markConversationAsRead - Could not mark conversation as read for userId %s and conversationId %s",
                            userId, conversationId),
                    newModelFailureCounter
            );

            CompletableFuture<Optional<ConversationResponse>> oldModelFuture = supplyAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.markConversationAsRead(userId, conversationId),
                    execSrvForOldModel,
                    String.format("OldModel markConversationAsRead - Could not mark conversation as read for userId %s and conversationId %s",
                            userId, conversationId),
                    oldModelFailureCounter
            );

            doDiff(diffConfig.useDiff(userId),
                    () -> diff.conversationResponseDiff(userId, conversationId, newModelFuture, oldModelFuture),
                    "Error diff-ing conversation responses");

            return (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public PostBoxResponse getConversations(String userId, Integer size, Integer page) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            CompletableFuture<PostBoxResponse> newModelFuture = supplyAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> postBoxResponseConverter.toPostBoxResponse(
                            newPostBoxService.getConversations(userId, Visibility.ACTIVE, page, size), page, size),
                    execSrvForNewModel,
                    String.format("NewModel getConversations - Could not get conversations for userId %s, visibility %s, page %d and size %d",
                            userId, Visibility.ACTIVE.name(), page, size),
                    newModelFailureCounter
            );

            CompletableFuture<PostBoxResponse> oldModelFuture = supplyAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.getConversations(userId, size, page),
                    execSrvForOldModel,
                    String.format("OldModel getConversations - Could not get conversations for userId %s, visibility %s, page %d and size %d",
                            userId, Visibility.ACTIVE.name(), page, size),
                    oldModelFailureCounter
            );

            doDiff(diffConfig.useDiff(userId),
                    () -> diff.postBoxResponseDiff(userId, newModelFuture, oldModelFuture),
                    "Error diff-ing postbox responses");

            return (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public PostBoxResponse deleteConversations(String userId, List<String> conversationIds, Integer size, Integer page) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            CompletableFuture<PostBoxResponse> newModelFuture = supplyAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> postBoxResponseConverter.toPostBoxResponse(
                            newPostBoxService.changeConversationVisibilities(userId, conversationIds,
                                    Visibility.ARCHIVED, Visibility.ACTIVE, page, size), page, size),
                    execSrvForNewModel,
                    String.format("NewModel deleteConversations - Could not delete conversations for userId %s, conversationIds %s, page %d and size %d",
                            userId, StringUtils.join(conversationIds, ", "), page, size),
                    newModelFailureCounter
            );

            CompletableFuture<PostBoxResponse> oldModelFuture = supplyAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.deleteConversations(userId, conversationIds, size, page),
                    execSrvForOldModel,
                    String.format("OldModel deleteConversations - Could not delete conversations for userId %s and conversationIds %s, page %d and size %d",
                            userId, StringUtils.join(conversationIds, ", "), page, size),
                    oldModelFailureCounter
            );

            doDiff(diffConfig.useDiff(userId),
                    () -> diff.postBoxResponseDiff(userId, newModelFuture, oldModelFuture),
                    "Error diff-ing postbox responses");

            return (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String userId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {

            CompletableFuture<PostBoxUnreadCounts> newModelFuture = supplyAsync(
                    newModelConfig.newModelEnabled(userId),
                    () -> unreadCountsConverter.toOldUnreadCounts(newPostBoxService.getUnreadCounts(userId)),
                    execSrvForNewModel,
                    String.format("NewModel getUnreadCounts - Could not get unread counts for userId %s", userId),
                    newModelFailureCounter
            );

            CompletableFuture<PostBoxUnreadCounts> oldModelFuture = supplyAsync(
                    oldModelEnabled,
                    () -> oldPostBoxService.getUnreadCounts(userId),
                    execSrvForOldModel,
                    String.format("NewModel getUnreadCounts - Could not get unread counts for userId %s", userId),
                    oldModelFailureCounter
            );

            doDiff(diffConfig.useDiff(userId),
                    () -> diff.postBoxUnreadCountsDiff(userId, newModelFuture, oldModelFuture),
                    "Error diff-ing unread counts");

            return (newModelConfig.useNewModel(userId) ? newModelFuture : oldModelFuture).join();
        }
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            return oldPostBoxService.getResponseData(userId);
        }
    }

    private void doDiff(boolean useDiff, Runnable runnable, String errorMessage) {
        if (useDiff) {
            CompletableFuture
                    .runAsync(runnable, execSrvForDiff)
                    .exceptionally(ex -> {
                        diffFailureCounter.inc();
                        DIFF_LOGGER.error(errorMessage, ex);
                        return null;
                    });
        }
    }

    private <T> CompletableFuture<T> supplyAsync(boolean condition, Supplier<T> supplier, ExecutorService execSrv,
                                                 String errorMessage, Counter failureCounter) {
        Supplier<T> _supplier = condition ? supplier : () -> null;

        CompletableFuture<T> returnFuture = CompletableFuture
                .supplyAsync(_supplier, execSrv)
                .exceptionally(ex -> {
                    failureCounter.inc();
                    LOGGER.error(errorMessage, ex);
                    throw new RuntimeException(ex);
                });

        logMetrics();

        return returnFuture;
    }

    private CompletableFuture runAsync(boolean condition, Runnable runnable, ExecutorService execSrv,
                                       String errorMessage, Counter failureCounter) {
        Runnable _runnable = condition ? runnable : () -> {
        };

        return CompletableFuture
                .runAsync(_runnable, execSrv)
                .exceptionally(ex -> {
                    failureCounter.inc();
                    LOGGER.error(errorMessage, ex);
                    throw new RuntimeException(ex);
                });
    }

    private ExecutorService newExecutorService(String metricsName) {
        String metricsOwner = "postBoxDelegatorService";
        int corePoolSize = getRuntime().availableProcessors();

        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(
                        corePoolSize, corePoolSize * 2,
                        60L, TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        new InstrumentedCallerRunsPolicy(metricsOwner, metricsName)
                ),
                metricsOwner,
                metricsName);
    }

    private ExecutorService newExecutorServiceForDiff() {
        return new InstrumentedExecutorService(
                Executors.newFixedThreadPool(getRuntime().availableProcessors()),
                "postBoxDelegatorService",
                "diffExecSrv"
        );
    }

    private void logMetrics() {
        MetricsService.getInstance().getRegistry().getCounters().forEach((name, counter) ->
                LOGGER.info("Counter with name: {} has value: ", name, counter.getCount()));
        MetricsService.getInstance().getRegistry().getGauges().forEach((name, gauge) ->
                LOGGER.info("Gauge with name: {} has value: ", name, gauge.getValue()));
    }
}