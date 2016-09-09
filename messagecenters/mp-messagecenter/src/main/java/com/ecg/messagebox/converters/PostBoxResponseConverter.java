package com.ecg.messagebox.converters;

import com.ecg.messagebox.converters.BuyerSellerInfo.BuyerSellerInfoBuilder;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.webapi.responses.MessageResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ecg.messagebox.converters.ConversationRoleUtil.getConversationRole;
import static com.ecg.messagecenter.util.MessageCenterUtils.truncateText;

@Component
public class PostBoxResponseConverter {

    private final MessageResponseConverter msgRespConverter;
    private final int msgTextMaxChars;

    @Autowired
    public PostBoxResponseConverter(MessageResponseConverter msgRespConverter,
                                    @Value("${replyts.maxPreviewMessageCharacters:250}") int msgTextMaxChars) {
        this.msgRespConverter = msgRespConverter;
        this.msgTextMaxChars = msgTextMaxChars;
    }

    public PostBoxResponse toPostBoxResponse(PostBox newPostBox, int page, int size) {
        PostBoxResponse pbResponse = new PostBoxResponse()
                .initNumUnreadMessages(newPostBox.getUnreadCounts().getNumUnreadMessages())
                .meta(newPostBox.getConversations().size(), page, size);

        String projectionOwnerUserId = newPostBox.getUserId();
        newPostBox.getConversations().forEach(conv ->
                {
                    BuyerSellerInfo bsInfo = new BuyerSellerInfoBuilder(conv.getParticipants()).build();
                    PostBoxListItemResponse itemResponse = new PostBoxListItemResponse(
                            conv.getId(),
                            bsInfo.getBuyerName(),
                            bsInfo.getSellerName(),
                            bsInfo.getBuyerId(),
                            bsInfo.getSellerId(),
                            conv.getAdId(),
                            getConversationRole(newPostBox.getUserId(), conv.getParticipants()),
                            conv.getNumUnreadMessages(),
                            msgRespWithTruncatedText(conv.getLatestMessage(), projectionOwnerUserId, conv.getParticipants())
                    );
                    pbResponse.addItem(itemResponse);
                }
        );

        return pbResponse;
    }

    private MessageResponse msgRespWithTruncatedText(Message message, String projectionOwnerUserId, List<Participant> convParticipants) {
        MessageResponse msgResp = msgRespConverter.toMessageResponse(message, projectionOwnerUserId, convParticipants);
        return new MessageResponse(
                msgResp.getReceivedDate(),
                msgResp.getBoundness(),
                truncateText(msgResp.getTextShortTrimmed(), msgTextMaxChars),
                msgResp.getSenderEmail()
        );
    }
}