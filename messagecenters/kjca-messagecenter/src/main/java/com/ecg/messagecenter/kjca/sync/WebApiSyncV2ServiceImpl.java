package com.ecg.messagecenter.kjca.sync;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.resources.WebApiSyncV2Service;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.sync.EmailNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "webapi.sync.v2.enabled", havingValue = "true")
public class WebApiSyncV2ServiceImpl implements WebApiSyncV2Service {

    private static final Logger LOG = LoggerFactory.getLogger(WebApiSyncV2ServiceImpl.class);

    private final PostBoxService postBoxService;
    private final ConversationService conversationService;

    @Autowired
    public WebApiSyncV2ServiceImpl(PostBoxService postBoxService, ConversationService conversationService) {
        this.postBoxService = postBoxService;
        this.conversationService = conversationService;

        LOG.info(this.getClass().getSimpleName() + " runs in V2 SyncMode");
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, String messageIdCursorOpt, int messagesLimit) throws InterruptedException {
        Optional<ConversationThread> result = postBoxService.markConversationAsRead(userId, conversationId, messageIdCursorOpt, messagesLimit);

        if (result.isPresent()) {
            try {
                ConversationThread conversationThread = result.get();
                String email = conversationThread.getParticipants().stream()
                        .filter(participant -> participant.getUserId().equals(userId))
                        .map(Participant::getEmail)
                        .findFirst()
                        .orElseThrow(() -> new EmailNotFoundException(String.format("Cannot find user email. UserId: %s, ConversationId: %s", userId, conversationId)));

                conversationService.readConversation(email, conversationId);
            } catch (Exception ex) {
                LOG.error("Synchronization of Reading Messages Failed", ex);
            }
        }

        return result;
    }

    @Override
    public PostBox archiveConversations(String userId, List<String> conversationIds, int offset, int limit) throws InterruptedException {
        if (!conversationIds.isEmpty()) {
            Optional<ConversationThread> conversation = postBoxService.getConversation(userId, conversationIds.get(0), null, 0);

            if (conversation.isPresent()) {
                try {
                    ConversationThread conversationThread = conversation.get();
                    String email = conversationThread.getParticipants().stream()
                            .filter(participant -> participant.getUserId().equals(userId))
                            .map(Participant::getEmail)
                            .findFirst()
                            .orElseThrow(() -> new EmailNotFoundException(String.format("Cannot find user email for delete a conversation. UserId: %s", userId)));

                    for (String conversationId : conversationIds) {
                        conversationService.deleteConversation(email, conversationId);
                    }
                } catch (Exception ex) {
                    LOG.error("Synchronization of Deleting Messages Failed", ex);
                }
            }
        }

        return postBoxService.archiveConversations(userId, conversationIds, offset, limit);
    }
}
