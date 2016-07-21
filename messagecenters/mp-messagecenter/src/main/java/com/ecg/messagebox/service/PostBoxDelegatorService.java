package com.ecg.messagebox.service;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagecenter.persistence.NewMessageListener;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostBoxDelegatorService implements PostBoxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBoxDelegatorService.class);

    private static final int MESSAGES_LIMIT = 500;

    private PostBoxService oldPostBoxService;
    private com.ecg.messagebox.service.PostBoxService newPostBoxService;

    private boolean writeToNewDataModel;
    private boolean readFromNewDataModel;

    private ExecutorService executorService;

    private final Timer processNewMessageTimer = TimingReports.newTimer("postBoxDelegatorService.processNewMessage");
    private final Timer getConversationTimer = TimingReports.newTimer("postBoxDelegatorService.getConversation");
    private final Timer markConversationAsReadTimer = TimingReports.newTimer("postBoxDelegatorService.markConversationAsRead");
    private final Timer markConversationsAsReadTimer = TimingReports.newTimer("postBoxDelegatorService.markConversationsAsRead");
    private final Timer getConversationsTimer = TimingReports.newTimer("postBoxDelegatorService.getConversations");
    private final Timer deleteConversationsTimer = TimingReports.newTimer("postBoxDelegatorService.deleteConversations");
    private final Timer getUnreadCountsTimer = TimingReports.newTimer("postBoxDelegatorService.getUnreadCounts");
    private final Timer getResponseDataTimer = TimingReports.newTimer("postBoxDelegatorService.getResponseData");

    @Autowired
    public PostBoxDelegatorService(@Qualifier("oldCassandraPostBoxService") PostBoxService oldPostBoxService,
                                   @Qualifier("newCassandraPostBoxService") com.ecg.messagebox.service.PostBoxService newPostBoxService,
                                   @Value("${messagebox.newDataModel.write.enabled:false}") boolean writeToNewDataModel,
                                   @Value("${messagebox.newDataModel.read.enabled:false}") boolean readFromNewDataModel) {

        this.oldPostBoxService = oldPostBoxService;
        this.newPostBoxService = newPostBoxService;

        this.writeToNewDataModel = writeToNewDataModel;
        this.readFromNewDataModel = readFromNewDataModel;

        if (writeToNewDataModel || readFromNewDataModel) {
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

    @Override
    public void processNewMessage(String postBoxId, Conversation rtsConversation, Message rtsMessage, ConversationRole conversationRole,
                                  boolean newReplyArrived, Optional<NewMessageListener> newMessageListenerOpt) {

        try (Timer.Context ignored = processNewMessageTimer.time()) {

            if (writeToNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.processNewMessage(postBoxId, rtsConversation, rtsMessage, conversationRole, newReplyArrived, newMessageListenerOpt), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Write ### - Could not process new message for postBoxId {}, conversationId {} and messageId {}",
                                    postBoxId, rtsConversation.getId(), rtsMessage.getId(), ex);
                            return null;
                        });
            }

            oldPostBoxService.processNewMessage(postBoxId, rtsConversation, rtsMessage, conversationRole, newReplyArrived, newMessageListenerOpt);
        }
    }

    @Override
    public Optional<ConversationResponse> getConversation(String postBoxId, String conversationId) {
        try (Timer.Context ignored = getConversationTimer.time()) {

            if (readFromNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.getConversation(postBoxId, conversationId, Optional.empty(), MESSAGES_LIMIT), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Read ### - Could not get conversation for postBoxId {} and conversationId {}",
                                    postBoxId, conversationId, ex);
                            return null;
                        });
            }

            return oldPostBoxService.getConversation(postBoxId, conversationId);
        }
    }

    @Override
    public Optional<ConversationResponse> markConversationAsRead(String postBoxId, String conversationId) {
        try (Timer.Context ignored = markConversationAsReadTimer.time()) {

            if (writeToNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.markConversationAsRead(postBoxId, conversationId, Optional.empty(), MESSAGES_LIMIT), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Write ### - Could not mark conversation as read for postBoxId {} and conversationId {}",
                                    postBoxId, conversationId, ex);
                            return null;
                        });
            }

            return oldPostBoxService.markConversationAsRead(postBoxId, conversationId);
        }
    }

    @Override
    public PostBoxResponse markConversationsAsRead(String postBoxId, Integer size, Integer page) {
        try (Timer.Context ignored = markConversationsAsReadTimer.time()) {

            // TODO

            return oldPostBoxService.markConversationsAsRead(postBoxId, size, page);
        }
    }

    @Override
    public PostBoxResponse getConversations(String postBoxId, Integer size, Integer page) {
        try (Timer.Context ignored = getConversationsTimer.time()) {

            if (readFromNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.getConversations(postBoxId, Visibility.ACTIVE, page, size), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Read ### - Could not get conversations for postBoxId {} and visibility {}",
                                    postBoxId, Visibility.ACTIVE.name(), ex);
                            return null;
                        });
            }

            return oldPostBoxService.getConversations(postBoxId, size, page);
        }
    }

    @Override
    public PostBoxResponse deleteConversations(String postBoxId, List<String> conversationIds, Integer page, Integer size) {
        try (Timer.Context ignored = deleteConversationsTimer.time()) {

            if (writeToNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.changeConversationVisibilities(postBoxId, conversationIds, Visibility.ARCHIVED, page, size), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Write ### - Could not archive conversations for postBoxId {} and conversationIds {}",
                                    postBoxId, StringUtils.join(conversationIds, ", "), ex);
                            return null;
                        });
            }

            return oldPostBoxService.deleteConversations(postBoxId, conversationIds, page, size);
        }
    }

    @Override
    public PostBoxUnreadCounts getUnreadCounts(String postBoxId) {
        try (Timer.Context ignored = getUnreadCountsTimer.time()) {

            if (readFromNewDataModel) {
                CompletableFuture
                        .runAsync(() ->
                                newPostBoxService.getUnreadCounts(postBoxId), executorService)
                        .exceptionally(ex -> {
                            LOGGER.error("### New Data Model - Read ### - Could not get unread counts for postBoxId {}", postBoxId, ex);
                            return null;
                        });
            }

            return oldPostBoxService.getUnreadCounts(postBoxId);
        }
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            // TODO: Implement it in new service as well
            return oldPostBoxService.getResponseData(userId);
        }
    }
}