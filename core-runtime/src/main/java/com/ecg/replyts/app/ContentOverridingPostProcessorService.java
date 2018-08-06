package com.ecg.replyts.app;

import com.ecg.replyts.app.postprocessorchain.ContentOverridingPostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessagesResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContentOverridingPostProcessorService {

    @Autowired(required = false)
    private MessagesResponseFactory messagesResponseFactory;

    @Autowired(required = false)
    private List<ContentOverridingPostProcessor> contentOverridingPostProcessors = new ArrayList<>();

    public String getCleanedMessage(Conversation conversation, Message message) {
        String cleanedMessage = messagesResponseFactory != null
                ? messagesResponseFactory.getCleanedMessage(conversation, message)
                : message.getPlainTextBody();

        for (ContentOverridingPostProcessor contentOverridingPostProcessor : contentOverridingPostProcessors) {
            cleanedMessage = contentOverridingPostProcessor.overrideContent(cleanedMessage);
        }

        return cleanedMessage;
    }
}
