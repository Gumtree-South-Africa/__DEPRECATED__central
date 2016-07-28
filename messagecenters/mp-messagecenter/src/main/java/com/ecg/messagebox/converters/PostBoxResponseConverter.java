package com.ecg.messagebox.converters;

import com.ecg.messagebox.converters.BuyerSellerInfo.BuyerSellerInfoBuilder;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.ecg.messagebox.converters.Utils.getConversationRole;

@Component
public class PostBoxResponseConverter {

    private final MessageResponseConverter msgRespConverter;

    @Autowired
    public PostBoxResponseConverter(MessageResponseConverter msgRespConverter) {
        this.msgRespConverter = msgRespConverter;
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
                            msgRespConverter.toMessageResponse(conv.getLatestMessage(), projectionOwnerUserId, conv.getParticipants()));
                    pbResponse.addItem(itemResponse);
                }
        );

        return pbResponse;
    }
}