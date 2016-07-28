package com.ecg.messagebox.converters;

import com.ecg.messagebox.converters.BuyerSellerInfo.BuyerSellerInfoBuilder;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.ecg.messagebox.converters.Utils.getConversationRole;

@Component
public class ConversationResponseConverter {

    private final MessageResponseConverter msgRespConverter;

    @Autowired
    public ConversationResponseConverter(MessageResponseConverter msgRespConverter) {
        this.msgRespConverter = msgRespConverter;
    }

    public ConversationResponse toConversationResponse(ConversationThread newConversation, String projectionOwnerUserId) {
        List<MessageResponse> messageResponses = newConversation.getMessages().stream()
                .map(msg -> msgRespConverter.toMessageResponse(msg, projectionOwnerUserId, newConversation.getParticipants()))
                .collect(Collectors.toList());

        BuyerSellerInfo bsInfo = new BuyerSellerInfoBuilder(newConversation.getParticipants()).build();

        return new ConversationResponse(
                newConversation.getId(),
                getConversationRole(projectionOwnerUserId, newConversation.getParticipants()),
                bsInfo.getBuyerEmail(),
                bsInfo.getSellerEmail(),
                bsInfo.getBuyerName(),
                bsInfo.getSellerName(),
                bsInfo.getBuyerId(),
                bsInfo.getSellerId(),
                newConversation.getAdId(),
                newConversation.getMetadata().getEmailSubject(),
                messageResponses,
                newConversation.getNumUnreadMessages());
    }
}