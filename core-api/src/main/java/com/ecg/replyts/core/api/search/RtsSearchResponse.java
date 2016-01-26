package com.ecg.replyts.core.api.search;

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
    }
}
