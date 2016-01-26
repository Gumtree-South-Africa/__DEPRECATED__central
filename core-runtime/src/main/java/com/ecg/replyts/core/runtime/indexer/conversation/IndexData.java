package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.indexer.MessageDocumentId;
import org.elasticsearch.common.xcontent.XContentBuilder;

class IndexData {

    private final Conversation conversation;
    private final Message message;
    private final XContentBuilder sourceBuilder;

    
    static final String DOCUMENT_TYPE = "message";

    public IndexData(Conversation conversation, Message message,
                     XContentBuilder sourceBuilder) {
        this.conversation = conversation;
        this.message = message;
        this.sourceBuilder = sourceBuilder;
    }

    public String getDocumentId() {
        return new MessageDocumentId(conversation.getId(), message.getId()).build();
    }

    public XContentBuilder getDocument() {
        return sourceBuilder;
    }

}
