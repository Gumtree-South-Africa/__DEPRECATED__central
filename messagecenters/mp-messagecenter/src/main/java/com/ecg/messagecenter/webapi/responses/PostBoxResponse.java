package com.ecg.messagecenter.webapi.responses;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostBoxResponse {

    private int numUnread;
    private Meta _meta;
    private List<PostBoxListItemResponse> conversations = new ArrayList<>();

    public PostBoxResponse initNumUnreadMessages(int numUnreadMessages) {
        this.numUnread = numUnreadMessages;
        return this;
    }

    public PostBoxResponse meta(int numFound, int currentPage, int pageSize) {
        _meta = new Meta();
        _meta.numFound = numFound;
        _meta.pageSize = pageSize;
        _meta.pageNum = currentPage;
        return this;
    }

    public PostBoxResponse addItem(PostBoxListItemResponse conversationListItem) {
        conversations.add(conversationListItem);
        return this;
    }

    public int getNumUnread() {
        return numUnread;
    }

    public Meta get_meta() {
        return _meta;
    }

    public List<PostBoxListItemResponse> getConversations() {
        return conversations;
    }

    public class Meta {

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Meta that = (Meta) o;
            return numFound == that.numFound
                    && pageSize == that.pageSize
                    && pageNum == that.pageNum;
        }

        @Override
        public int hashCode() {
            return Objects.hash(numFound, pageSize, pageNum);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("numFound", numFound)
                    .add("pageSize", pageSize)
                    .add("pageNum", pageNum)
                    .toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostBoxResponse that = (PostBoxResponse) o;
        return numUnread == that.numUnread
                && Objects.equals(_meta, that._meta)
                && Objects.equals(conversations, that.conversations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numUnread, _meta, conversations);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("numUnread", numUnread)
                .add("_meta", _meta)
                .add("conversations", conversations)
                .toString();
    }
}