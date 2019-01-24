package com.ecg.messagecenter.gtau.diff;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.resources.WebApiSyncV2Service;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
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
    private final SimpleMessageCenterRepository postBoxRepository;
    private final ConversationThreadService conversationThreadService;

    @Autowired
    public WebApiSyncV2ServiceImpl(PostBoxService postBoxService, SimpleMessageCenterRepository postBoxRepository, ConversationThreadService conversationThreadService) {
        this.postBoxService = postBoxService;
        this.postBoxRepository = postBoxRepository;
        this.conversationThreadService = conversationThreadService;
    }

    @Override
    public Optional<ConversationThread> markConversationAsRead(String userId, String conversationId, String messageIdCursorOpt, int messagesLimit) throws InterruptedException {
        Optional<ConversationThread> result = postBoxService.markConversationAsRead(userId, conversationId, messageIdCursorOpt, messagesLimit);
        if (result.isPresent()) {
            ConversationThread conversationThread = result.get();
            String email = conversationThread.getParticipants().stream()
                    .filter(participant -> participant.getUserId().equals(userId))
                    .map(Participant::getEmail)
                    .findFirst()
                    .orElseThrow(() -> new EmailNotFoundException(String.format("Cannot find user email. UserId: %s, ConversationId: %s", userId, conversationId)));

            conversationThreadService.markReadPostBoxConversation(email, conversationId, true, null);
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

                    // MessageCenter part
                    com.ecg.messagecenter.core.persistence.simple.PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
                    for (String conversationId : conversationIds) {
                        postBox.removeConversation(conversationId);
                    }
                    postBoxRepository.deleteConversations(postBox, conversationIds);

                } catch (Exception ex) {
                    LOG.error("Synchronization of Deleting Messages Failed", ex);
                }
            }
        }

        return postBoxService.archiveConversations(userId, conversationIds, offset, limit);
    }
}
