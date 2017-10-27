package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.TopLevelExceptionHandler;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeleteConversationCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterReportConversationCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.domain.user_message_report.ApiFlaggedConversation;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationThreadController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Timer API_POSTBOX_CONVERSATION_BY_ID_RIAK = TimingReports.newTimer("webapi-postbox-conversation-by-id-riak");
    private static final Timer API_POSTBOX_CONVERSATION_BY_ID_MSG_PROCESSING = TimingReports.newTimer("webapi-postbox-conversation-by-id-message-processing");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    private final SimplePostBoxRepository postBoxRepository;
    private final MutableConversationRepository conversationRepository;
    private final SearchIndexer searchIndexer;
    private final DomainObjectConverter converter;
    private final GumshieldApi gumshieldApi;
    private final ConversationEventListeners conversationEventListeners;

    @Autowired
    public ConversationThreadController(
            MutableConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository,
            SearchIndexer searchIndexer,
            MailCloakingService mailCloakingService,
            GumshieldApi gumshieldApi,
            ConversationEventListeners conversationEventListeners) {
        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
        this.searchIndexer = searchIndexer;
        this.converter = new DomainObjectConverter(conversationRepository, mailCloakingService);
        this.gumshieldApi = gumshieldApi;
        this.conversationEventListeners = conversationEventListeners;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        TopLevelExceptionHandler.handle(ex, response);
    }

    @PostMapping(MessageCenterReportConversationCommand.MAPPING)
    ResponseEntity<ResponseObject<?>> reportConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return entityNotFound();
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, ConversationCustomValue.AT_POSTFIX, now.toString()));
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, ConversationCustomValue.DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        markConversationAsRead(conversationId, postBox);

        ConversationRts conversationRts = converter.convertConversation(conversation);

        ApiFlaggedConversation flaggedConversation = new ApiFlaggedConversation();
        flaggedConversation.setAdvertId(Long.valueOf(conversationRts.getAdId()));
        flaggedConversation.setConversationId(conversationRts.getId());
        flaggedConversation.setReportedByEmail(email);
        flaggedConversation.setReportedDate(DateTime.parse(getFlaggedTime(conversationRts, email)));
        flaggedConversation.setReportedForEmail(getOtherEmail(conversationRts, email));
        gumshieldApi.conversationApi().flagConversation(flaggedConversation);
        return ResponseEntity.ok(new ResponseObject<>());
    }

    private String getOtherEmail(ConversationRts conversation, String email) {
        return conversation.getBuyer().equalsIgnoreCase(email)
                ? conversation.getSeller()
                : conversation.getBuyer();
    }

    private String getFlaggedTime(ConversationRts conversation, String email) {
        Map<String, String> conversationHeaders = conversation.getConversationHeaders();
        return conversation.getBuyer().equalsIgnoreCase(email)
                ? conversationHeaders.get("flagged-buyer-at")
                : conversationHeaders.get("flagged-seller-at");
    }

    @DeleteMapping(MessageCenterDeleteConversationCommand.MAPPING)
    ResponseEntity<ResponseObject<?>> deleteConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return entityNotFound();
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, ConversationCustomValue.AT_POSTFIX, now.toString()));
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, ConversationCustomValue.DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        markConversationAsRead(conversationId, postBox);

        ConversationRts conversationRts = converter.convertConversation(conversation);
        return ResponseEntity.ok(ResponseObject.of(conversationRts));
    }


    private void markConversation(MutableConversation conversation, ConversationCustomValue customValue) {
        conversation.applyCommand(new AddCustomValueCommand(conversation.getId(), customValue.keyName(), customValue.value()));
    }

    private ConversationRole emailBelongsToBuyerOrSeller(Conversation conversation, String email) {
        if (conversation != null) {
            if (conversation.getBuyerId().equalsIgnoreCase(email)) {
                return ConversationRole.Buyer;
            } else if (conversation.getSellerId().equalsIgnoreCase(email)) {
                return ConversationRole.Seller;
            }
        }
        return null;
    }

    /*
     * newCounterMode is never used, default is always used = true
     */
    @GetMapping(MessageCenterGetPostBoxConversationCommand.MAPPING)
    ResponseEntity<ResponseObject<?>> getConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        try (Timer.Context ignore = API_POSTBOX_CONVERSATION_BY_ID.time()) {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound();
            }

            return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId);
        }
    }

    /*
     * newCounterMode is never used, default is always used = true
     */
    @PutMapping(MessageCenterGetPostBoxConversationCommand.MAPPING)
    ResponseEntity<ResponseObject<?>> readConversation(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId) {

        try (Timer.Context ignore = API_POSTBOX_CONVERSATION_BY_ID.time()) {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound();
            }

            if (conversationThreadRequested.get().isContainsUnreadMessages()) {
                int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
                postBox.decrementNewReplies(unreadMessages);
                postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
            }

            return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId);
        }
    }

    private ResponseEntity<ResponseObject<?>> lookupConversation(long numUnread, String email, String conversationId) {
        Conversation conversation;
        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_BY_ID_RIAK.time()) {
            conversation = conversationRepository.getById(conversationId);
        }

        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return entityNotFound();
        }

        Optional<PostBoxSingleConversationThreadResponse> created;
        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_BY_ID_MSG_PROCESSING.time()) {
            created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation);
        }

        if (created.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
            return ResponseEntity.ok(ResponseObject.of(created.get()));
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound();
        }

    }

    private void markConversationAsRead(String conversationId, PostBox<ConversationThread> postBox) {
        for (ConversationThread item : postBox.getConversationThreads()) {
            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                postBoxRepository.markConversationAsRead(postBox, item);
                break;
            }
        }
    }

    private ResponseEntity<ResponseObject<?>> entityNotFound() {
        return new ResponseEntity<>(ResponseObject.of(RequestState.ENTITY_NOT_FOUND), HttpStatus.NOT_FOUND);
    }
}