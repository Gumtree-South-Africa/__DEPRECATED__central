package com.ecg.messagecenter.webapi.responses;

import com.ecg.gumtree.replyts2.common.message.MessageCenterUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class PostBoxResponse {
    private Integer numUnread;
    private DateTime lastModified;
    private Meta meta;
    private List<PostBoxListItemResponse> conversations = new ArrayList<PostBoxListItemResponse>();

    public PostBoxResponse addItem(PostBoxListItemResponse conversationListItem) {
        conversations.add(conversationListItem);
        return this;
    }

    public PostBoxResponse initNumUnread(Integer num) {
        this.numUnread = num;
        return this;
    }

    public PostBoxResponse initLastModified(DateTime lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public PostBoxResponse meta(int numFound, int currentPage, int pageSize) {
        meta = new Meta();
        meta.numFound = numFound;
        meta.pageSize = pageSize;
        meta.pageNum = currentPage;
        return this;
    }

    public List<PostBoxListItemResponse> getConversations() {
        return conversations;
    }

    public Integer getNumUnread() {
        return numUnread;
    }

    public String getLastModified() {
        return MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(lastModified);
    }

    public Meta get_meta() {
        // NOTE: method name required for _meta to be returned in the JSON response.
        return meta;
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