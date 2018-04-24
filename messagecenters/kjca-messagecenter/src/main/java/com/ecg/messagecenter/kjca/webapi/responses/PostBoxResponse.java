package com.ecg.messagecenter.kjca.webapi.responses;


import com.ecg.messagecenter.util.MessageCenterUtils;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* User: maldana
* Date: 30.10.13
* Time: 17:14
*
* @author maldana@ebay.de
*/
public class PostBoxResponse {

    private Integer numUnread;
    private Map<ConversationRole, Integer> numUnreadPerRole = new HashMap<>();
    private DateTime lastModified;
    private Meta _meta;
    private List<PostBoxListItemResponse> conversations = new ArrayList<>();

    public PostBoxResponse addItem(PostBoxListItemResponse conversationListItem) {
        conversations.add(conversationListItem);
        return this;
    }

    public PostBoxResponse initNumUnread(Integer num, Integer numUnreadAsBuyer, Integer numUnreadAsSeller, DateTime lastModified) {
        this.numUnread = num;
        this.numUnreadPerRole.put(ConversationRole.Buyer, numUnreadAsBuyer);
        this.numUnreadPerRole.put(ConversationRole.Seller, numUnreadAsSeller);
        this.lastModified = lastModified;
        return this;
    }

    public void meta(int numFound, int currentPage, int pageSize) {
        _meta = new Meta();
        _meta.numFound = numFound;
        _meta.pageSize = pageSize;
        _meta.pageNum = currentPage;
    }

    public List<PostBoxListItemResponse> getConversations() {
        return conversations;
    }

    public Integer getNumUnread() {
        return numUnread;
    }

    public Map<ConversationRole, Integer> getNumUnreadPerRole() {
        return numUnreadPerRole;
    }

    public String getLastModified() {
        return MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(lastModified);
    }

    public Meta get_meta() {
        return _meta;
    }

    class Meta {
        private int numFound;
        private int pageSize;
        private int pageNum;

        public int getNumFound() {
            return numFound;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getPageNum() {
            return pageNum;
        }
    }
}
