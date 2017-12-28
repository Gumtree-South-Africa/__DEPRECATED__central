package com.ecg.replyts.core.api.search;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;

public class RtsSearchResponse {
    public static final int NOT_APPLICABLE = -1;

    private final List<IDHolder> result;
    private int offset = NOT_APPLICABLE;
    private int count = NOT_APPLICABLE;
    private int total = NOT_APPLICABLE;

    public RtsSearchResponse(List<IDHolder> result) {
        this(result, NOT_APPLICABLE, NOT_APPLICABLE, NOT_APPLICABLE);
    }

    public RtsSearchResponse(List<IDHolder> result, int offset, int count, int total) {
        //check validity
        this.result = result;
        this.offset = offset;
        this.count = count;
        this.total = total;
        if (anyPagingValueSet() && anyPagingValueNotSet()) {
            throw new IllegalArgumentException("If either pagination information value is set, the other two must be set, too.");
        }
    }

    private boolean anyPagingValueNotSet() {
        return (offset == NOT_APPLICABLE || count == NOT_APPLICABLE || total == NOT_APPLICABLE);
    }

    private boolean anyPagingValueSet() {
        return (offset != NOT_APPLICABLE || count != NOT_APPLICABLE || total != NOT_APPLICABLE);
    }

    public List<IDHolder> getResult() {
        return result;
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }

    public int getTotal() {
        return total;
    }

    public boolean isPartialResult() {
        return (offset != NOT_APPLICABLE && count != NOT_APPLICABLE && total != NOT_APPLICABLE) && count != total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RtsSearchResponse)) return false;
        RtsSearchResponse that = (RtsSearchResponse) o;
        return offset == that.offset &&
                count == that.count &&
                total == that.total &&
                Objects.equal(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(result, offset, count, total);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("result", result)
                .add("offset", offset)
                .add("count", count)
                .add("total", total)
                .toString();
    }

    public static class IDHolder {

        private String messageId;
        private String conversationId;

        public IDHolder(String messageId, String conversationId) {
            this.messageId = messageId;
            this.conversationId = conversationId;
        }

        public String getConversationId() {
            return conversationId;
        }

        public String getMessageId() {
            return messageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IDHolder)) return false;
            IDHolder idHolder = (IDHolder) o;
            return Objects.equal(messageId, idHolder.messageId) &&
                    Objects.equal(conversationId, idHolder.conversationId);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(messageId, conversationId);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("messageId", messageId)
                    .add("conversationId", conversationId)
                    .toString();
        }
    }
}
