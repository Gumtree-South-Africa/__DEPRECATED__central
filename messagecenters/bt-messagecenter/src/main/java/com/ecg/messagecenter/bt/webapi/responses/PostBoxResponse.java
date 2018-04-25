package com.ecg.messagecenter.bt.webapi.responses;

import com.ecg.messagecenter.util.MessageCenterUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class PostBoxResponse {
    private Integer numUnread;
    private DateTime lastModified;
    private Meta _meta;
    private List<PostBoxListItemResponse> conversations = new ArrayList<PostBoxListItemResponse>();

    public PostBoxResponse addItem(PostBoxListItemResponse conversationListItem) {
        conversations.add(conversationListItem);
        return this;
    }

    public PostBoxResponse initNumUnread(Integer num, DateTime lastModified) {
        this.numUnread = num;
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