package com.ecg.sync;

import com.ecg.messagecenter.core.util.MessageCenterUtils;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class PostBoxResponse<T> {

    private Integer numUnread;
    private DateTime lastModified;
    private Meta _meta;
    private List<T> conversations = new ArrayList<>();

    public PostBoxResponse addItem(T conversationListItem) {
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
        _meta = new Meta();
        _meta.numFound = numFound;
        _meta.pageSize = pageSize;
        _meta.pageNum = currentPage;
        return this;
    }

    public List<T> getConversations() {
        return conversations;
    }

    public Integer getNumUnread() {
        return numUnread;
    }

    public String getLastModified() {
        return MessageCenterUtils.toFormattedTimeISO8601ExplicitTimezoneOffset(lastModified);
    }

    // NOTE: method name required for _meta to be returned in the JSON response.
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
