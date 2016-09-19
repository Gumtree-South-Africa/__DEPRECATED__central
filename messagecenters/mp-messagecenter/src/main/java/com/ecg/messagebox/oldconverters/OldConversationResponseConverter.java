package com.ecg.messagebox.oldconverters;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.oldconverters.BuyerSellerInfo.BuyerSellerInfoBuilder;
import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.ecg.messagebox.oldconverters.ConversationRoleUtil.getConversationRole;

@Component("oldConversationResponseConverter")
public class OldConversationResponseConverter {

    private final OldMessageResponseConverter msgRespConverter;

    @Autowired
    public OldConversationResponseConverter(OldMessageResponseConverter msgRespConverter) {
        this.msgRespConverter = msgRespConverter;
    }

    public ConversationResponse toConversationResponse(ConversationThread newConversation, String projectionOwnerUserId) {
        List<MessageResponse> messageResponses = newConversation.getMessages().stream()
                .map(msg -> msgRespConverter.toMessageResponse(msg, projectionOwnerUserId, newConversation.getParticipants()))
                .collect(Collectors.toList());

        BuyerSellerInfo bsInfo = new BuyerSellerInfoBuilder(newConversation.getParticipants()).build();

        String creationDateStr = newConversation.getMetadata().getCreationDate()
                .map(MessageCenterUtils::toFormattedTimeISO8601ExplicitTimezoneOffset).orElse(null);

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
                creationDateStr,
                newConversation.getMetadata().getEmailSubject(),
                messageResponses,
                newConversation.getNumUnreadMessages());
    }
}