package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ConversationsResponse;
import com.ecg.messagebox.model.PostBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.util.stream.Collectors.toList;

@Component
public class ConversationsResponseConverter {

    private final ConversationResponseConverter convRespConverter;

    @Autowired
    public ConversationsResponseConverter(ConversationResponseConverter convRespConverter) {
        this.convRespConverter = convRespConverter;
    }

    public ConversationsResponse toConversationsResponse(PostBox postBox, int offset, int limit) {
        return new ConversationsResponse(
                postBox.getUserId(),
                postBox.getUnreadCounts().getNumUnreadMessages(),
                postBox.getUnreadCounts().getNumUnreadConversations(),
                postBox.getConversations().stream().map(convRespConverter::toConversationResponse).collect(toList()),
                offset,
                limit,
                postBox.getConversationsTotalCount()
        );
    }
}