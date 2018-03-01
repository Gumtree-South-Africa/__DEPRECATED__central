package com.ebay.ecg.replyts.robot.service;

import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.RichMessage;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import net.sf.json.JSONSerializer;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.storage.StorageBodyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.AD;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.FROM;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.MESSAGE_LINKS;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.MESSAGE_SENDER;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.REPLY_CHANNEL;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.RICH_TEXT_LINKS;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.RICH_TEXT_MESSAGE;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.ROBOT;
import static com.ebay.ecg.replyts.robot.service.ContentUtils.Header.SUBJECT;
import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

public final class ContentUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ContentUtils.class);

    private ContentUtils() {
    }

    public static Optional<Mail> buildMail(Conversation conversation, MessagePayload payload) {
        try {
            return Optional.of(buildMailContent(conversation, payload));
        } catch (MimeException me) {
            LOG.error("Couldn't build mail for ad {}", conversation.getAdId(), me);
            return Optional.empty();
        }
    }

    public static AddMessageCommand buildAddMessage(MutableConversation conversation, MessagePayload payload, String messageId) {
        AddMessageCommandBuilder builder = anAddMessageCommand(conversation.getId(), messageId)
                .withTextParts(Collections.singletonList(payload.getMessage()))
                .withMessageDirection(payload.getMessageDirection())
                .addHeader(ROBOT.key, ROBOT.value)
                .addHeader(REPLY_CHANNEL.key, REPLY_CHANNEL.value)
                .addHeader(MESSAGE_LINKS.key, JSONSerializer.toJSON(payload.getLinks()).toString());

        if (payload.getSender() != null) {
            builder.addHeader(MESSAGE_SENDER.key, JSONSerializer.toJSON(payload.getSender()).toString());
        }

        RichMessage richTextMessage = payload.getRichTextMessage();
        if (richTextMessage != null) {
            builder.addHeader(RICH_TEXT_MESSAGE.key, richTextMessage.getRichMessageText());
            builder.addHeader(RICH_TEXT_LINKS.key, JSONSerializer.toJSON(richTextMessage.getLinks()).toString());
        }

        return builder.build();
    }

    private static Mail buildMailContent(Conversation conversation, MessagePayload payload) throws MimeException {
        Message mail = new DefaultMessageBuilder().newMessage();
        mail.setFrom(AddressBuilder.DEFAULT.parseMailbox(FROM.value));
        mail.setTo(AddressBuilder.DEFAULT.parseMailbox(conversation.getSellerId()), AddressBuilder.DEFAULT.parseMailbox(conversation.getBuyerId()));
        mail.setDate(new Date());
        mail.setSubject(SUBJECT.value);
        mail.setBody(new StorageBodyFactory().textBody(payload.getMessage()));
        mail.setHeader(buildMailHeader(conversation.getAdId(), payload));
        return new StructuredMail(mail);
    }

    private static org.apache.james.mime4j.dom.Header buildMailHeader(String adId, MessagePayload payload) throws MimeException {
        org.apache.james.mime4j.dom.Header header = new HeaderImpl();
        header.addField(buildHeader(FROM.key, FROM.value));
        header.addField(buildHeader(ROBOT.key, ROBOT.value));
        header.addField(buildHeader(AD.key, adId));
        header.addField(buildHeader(REPLY_CHANNEL.key, REPLY_CHANNEL.value));
        header.addField(buildHeader(MESSAGE_LINKS.key, JSONSerializer.toJSON(payload.getLinks()).toString()));

        if (payload.getSender() != null) {
            header.addField(buildHeader(MESSAGE_SENDER.key, JSONSerializer.toJSON(payload.getSender()).toString()));
        }

        RichMessage richTextMessage = payload.getRichTextMessage();
        if (richTextMessage != null) {
            header.addField(buildHeader(RICH_TEXT_MESSAGE.key, richTextMessage.getRichMessageText()));
            header.addField(buildHeader(RICH_TEXT_LINKS.key, JSONSerializer.toJSON(richTextMessage.getLinks()).toString()));
        }

        return header;
    }

    private static ParsedField buildHeader(String key, String value) throws MimeException {
        return DefaultFieldParser.parse(String.format("%s: %s", key, value));
    }

    enum Header {

        ROBOT("X-Robot", "GTAU"),
        FROM("From", "noreply@gumtree.com.au"),
        AD("X-ADID", "0000"),
        SUBJECT("Subject", "Gumtree Robot"),
        REPLY_CHANNEL("X-Reply-Channel", "gumbot"),
        MESSAGE_LINKS("X-Message-Links", "{}"),
        RICH_TEXT_MESSAGE("X-RichText-Message", ""),
        RICH_TEXT_LINKS("X-RichText-Links", "{}"),
        MESSAGE_SENDER("X-Message-Sender", "{}");

        private final String key;
        private final String value;

        Header(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
