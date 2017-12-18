package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * For each message, the data indexed is:
 * <ul>
 * <li>Message ID</li>
 * <li>Conversation ID</li>
 * <li>"From" Mail Address</li>
 * <li>"To" Mail Address</li>
 * <li>Buyer Secret</li>
 * <li>Seller Secret</li>
 * <li>Message Direction</li>
 * <li>Message State</li>
 * <li>Filter Result State</li>
 * <li>Moderation Result State</li>
 * <li>Message Received Date</li>
 * <li>Conversation Creation Date</li>
 * <li>Conversation Custom Headers</li>
 * <li>Message Text</li>
 * <li>Message Subject</li>
 * <li>Ad ID</li>
 * <li>Processing Feedback</li>
 * <li>attachment filenames</li>
 * </ul>
 */
public class IndexDataBuilder {

    private MailCloakingService mailCloakingService;

    public IndexDataBuilder(MailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    public IndexData toIndexData(Conversation conversation,
                                 Message message) throws IOException {

        List<String> attachmentFilenames = message.getAttachmentFilenames();

        // Email addresses are lowercased, since the field is not analyzed
        // and API searches lowercase explicitly.
        // See com.ecg.replyts.app.search.elasticsearch.SearchTransformer.setupMailAddressQuery()
        XContentBuilder sourceBuilder = jsonBuilder()
                .startObject()
                .field("toEmail", lowercase(getToEmail(conversation, message)))
                .field("toEmailAnonymous", lowercase(getToAnonymousEmail(conversation, message)))
                .field("fromEmail", lowercase(getFromEmail(conversation, message)))
                .field("fromEmailAnonymous", lowercase(getFromAnonymousEmail(conversation, message)))
                .field("messageDirection", message.getMessageDirection())
                .field("messageState", message.getState())
                .field("humanResultState", message.getHumanResultState())
                .field("receivedDate", message.getReceivedAt().toDate())
                .field("conversationStartDate", conversation.getCreatedAt().toDate())
                .field("messageText", message.getPlainTextBody())
                .field("adId", conversation.getAdId())
                .field("attachments", attachmentFilenames.toArray(new String[attachmentFilenames.size()]))
                .field("lastEditor", message.getLastEditor().orElse(null))
                .field("lastModified", message.getLastModifiedAt().toDate())

                .startObject("customHeaders");
        for (Map.Entry<String, String> h : conversation.getCustomValues().entrySet()) {
            sourceBuilder.field(h.getKey(), h.getValue());
        }

        sourceBuilder.endObject().startArray("feedback");

        for (ProcessingFeedback feedback : message.getProcessingFeedback()) {
            sourceBuilder.startObject()
                    .field("filterName", feedback.getFilterName())
                    .field("filterInstance", feedback.getFilterInstance())
                    .endObject();
        }

        sourceBuilder.endArray().endObject();

        return new IndexData(conversation, message, sourceBuilder);
    }

    private String getFromAnonymousEmail(Conversation conversation, Message message) {
        return mailCloakingService.createdCloakedMailAddress(message.getMessageDirection().getFromRole(), conversation).getAddress();
    }

    private String getToAnonymousEmail(Conversation conversation, Message message) {
        return mailCloakingService.createdCloakedMailAddress(message.getMessageDirection().getToRole(), conversation).getAddress();
    }

    private static String getFromEmail(Conversation conversation, Message message) {
        switch (message.getMessageDirection()) {
            case BUYER_TO_SELLER:
                return conversation.getBuyerId();
            case SELLER_TO_BUYER:
                return conversation.getSellerId();
            case UNKNOWN:
                return null;
            default:
                throw new AssertionError("Unknown message direction: " + message.getMessageDirection());
        }
    }

    private static String getToEmail(Conversation conversation, Message message) {
        switch (message.getMessageDirection()) {
            case BUYER_TO_SELLER:
                return conversation.getSellerId();
            case SELLER_TO_BUYER:
                return conversation.getBuyerId();
            case UNKNOWN:
                return null;
            default:
                throw new AssertionError("Unknown message direction: " + message.getMessageDirection());
        }
    }

    private static String lowercase(String string) {
        return string == null ? null : string.toLowerCase();
    }
}