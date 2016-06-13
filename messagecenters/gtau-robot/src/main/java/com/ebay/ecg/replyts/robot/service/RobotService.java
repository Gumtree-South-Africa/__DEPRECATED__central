package com.ebay.ecg.replyts.robot.service;

import com.ebay.ecg.replyts.robot.api.exception.ConversationNotFoundException;
import com.ebay.ecg.replyts.robot.api.requests.payload.GetConversationsResponsePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

/**
 * Created by mdarapour.
 */
@Service
public class RobotService {
    private final static Logger LOGGER = LoggerFactory.getLogger(RobotService.class);

    private final MutableConversationRepository conversationRepository;
    private final ModerationService moderationService;
    private final MailRepository mailRepository;
    private final SearchService searchService;
    private final Guids guids;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired
    public RobotService(MutableConversationRepository conversationRepository,
                        ModerationService moderationService,
                        MailRepository mailRepository,
                        SearchService searchService,
                        Guids guids) {

        this.conversationRepository = conversationRepository;
        this.moderationService = moderationService;
        this.mailRepository = mailRepository;
        this.searchService = searchService;
        this.guids = guids;
    }

    public void addMessageToConversation(String conversationId, MessagePayload payload) throws IOException, MimeException {
        Optional<MutableConversation> conversation = Optional.fromNullable(conversationRepository.getById(conversationId));
        // We don't want to send any message to invalid or inactive conversations
        if (!conversation.isPresent() || !ConversationState.ACTIVE.equals(conversation.get().getState())) {
            throw new ConversationNotFoundException("Conversation " + conversationId + " not found.");
        }

        updateConversation(conversation.get(), payload);
    }

    public List<String> addMessageToConversations(Set<String> conversationIds, MessagePayload payload) {
        List<String> errors = Lists.newArrayList();
        for (String conversationId : conversationIds) {
            try {
                addMessageToConversation(conversationId, payload);
            } catch (Exception e) {
                LOGGER.error("Failed to add message to conversation", e);
                errors.add(conversationId);
            }
        }
        return errors;
    }

    public List<String> addMessageToConversationsForAd(String email, String adId, MessagePayload payload) {
        return addMessageToConversations(getUniqueConversationIdsForAd(email, adId, SearchMessagePayload.ConcernedUserRole.RECEIVER), payload);
    }

    public GetConversationsResponsePayload getConversationsForAd(String email, String adId) {
        GetConversationsResponsePayload responsePayload = new GetConversationsResponsePayload();
        responsePayload.setAdId(adId);
        responsePayload.setConversationIds(Lists.newArrayList(getConversationIdsWithNoSellerReplyForAd(email, adId)));
        return responsePayload;
    }

    private SearchMessagePayload searchForAllMessagesOfAdByRole(String email, String adId, SearchMessagePayload.ConcernedUserRole role) {
        SearchMessagePayload smp = new SearchMessagePayload();
        smp.setMessageState(MessageRtsState.SENT);
        smp.setAdId(adId);
        smp.setUserEmail(email);
        smp.setUserRole(role);
        return smp;
    }

    private Set<String> getUniqueConversationIdsForAd(String email, String adId, SearchMessagePayload.ConcernedUserRole role) {
        RtsSearchResponse response = searchService.search(searchForAllMessagesOfAdByRole(email, adId, role));
        Set<String> uniqueConversationIds = Sets.newHashSet();
        for (RtsSearchResponse.IDHolder id : response.getResult()) {
            if (uniqueConversationIds.contains(id.getConversationId())) {
                continue;
            }
            uniqueConversationIds.add(id.getConversationId());
        }
        return uniqueConversationIds;
    }

    private Set<String> getConversationIdsWithNoSellerReplyForAd(String email, String adId) {
        Set<String> buyer = getUniqueConversationIdsForAd(email, adId, SearchMessagePayload.ConcernedUserRole.RECEIVER);
        Set<String> seller = getUniqueConversationIdsForAd(email, adId, SearchMessagePayload.ConcernedUserRole.SENDER);
        buyer.removeAll(seller);
        return buyer;
    }

    private void updateConversation(MutableConversation conversation, MessagePayload payload) throws IOException, MimeException {
        String messageId = guids.nextGuid();
        Mails mails = new Mails();

        AddMessageCommandBuilder builder = anAddMessageCommand(conversation.getId(), messageId)
                .withTextParts(Arrays.asList(payload.getMessage()))
                .withMessageDirection(payload.getMessageDirectionEnum())
                .addHeader(Header.Robot.key, Header.Robot.value)
                .addHeader(Header.ReplyChannel.key, Header.ReplyChannel.value);

        builder.addHeader(Header.MessageLinks.key, JSONSerializer.toJSON(payload.getLinks()).toString());
        conversation.applyCommand(builder.build());

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        mailRepository.persistMail(messageId, mails.writeToBuffer(aRobotMail(conversation, payload)), Optional.<byte[]>absent());

        moderationService.changeMessageState(conversation, messageId, new ModerationAction(ModerationResultState.GOOD, Optional.<String>absent()));
    }

    private Mail aRobotMail(Conversation conversation, MessagePayload payload) throws MimeException {
        Message mail = new DefaultMessageBuilder().newMessage();
        org.apache.james.mime4j.dom.Header header = new HeaderImpl();
        mail.setFrom(AddressBuilder.DEFAULT.parseMailbox(Header.From.value));
        mail.setTo(AddressBuilder.DEFAULT.parseMailbox(conversation.getSellerId()), AddressBuilder.DEFAULT.parseMailbox(conversation.getBuyerId()));
        mail.setDate(new Date());
        mail.setSubject(Header.Subject.value);
        mail.setBody(new StorageBodyFactory().textBody(payload.getMessage()));
        header.addField(buildHeader(Header.From.key, Header.From.value));
        header.addField(buildHeader(Header.Robot.key, Header.Robot.value));
        header.addField(buildHeader(Header.Ad.key, conversation.getAdId()));
        header.addField(buildHeader(Header.ReplyChannel.key, Header.ReplyChannel.value));
        header.addField(buildHeader(Header.MessageLinks.key, JSONSerializer.toJSON(payload.getLinks()).toString()));
        mail.setHeader(header);
        return new StructuredMail(mail);
    }

    private ParsedField buildHeader(String key, String value) throws MimeException {
        return DefaultFieldParser.parse(String.format("%s: %s", key, value));
    }

    enum Header {
        Robot("X-Robot", "GTAU"),
        From("From", "noreply@gumtree.com.au"),
        Ad("X-ADID", "0000"),
        Subject("Subject", "Gumtree Robot"),
        ReplyChannel("X-Reply-Channel", "gumbot"),
        MessageLinks("X-Message-Links", "{}");

        private String key;
        private String value;

        Header(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
