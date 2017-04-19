package com.ecg.de.ebayk.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterDeleteConversationCommand;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterReportConversationCommand;
import com.ecg.de.ebayk.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.app.ConversationEventListeners;
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
import com.google.common.base.Optional;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.domain.user_message_report.ApiFlaggedConversation;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.ecg.de.ebayk.messagecenter.webapi.ConversationCustomValue.AT_POSTFIX;
import static com.ecg.de.ebayk.messagecenter.webapi.ConversationCustomValue.DATE_POSTFIX;
import static org.joda.time.DateTime.now;

@Controller
public class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationThreadController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Timer API_POSTBOX_CONVERSATION_BY_ID_RIAK = TimingReports.newTimer("webapi-postbox-conversation-by-id-riak");
    private static final Timer API_POSTBOX_CONVERSATION_BY_ID_MSG_PROCESSING = TimingReports.newTimer("webapi-postbox-conversation-by-id-message-processing");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");

    private final PostBoxRepository postBoxRepository;
    private final MutableConversationRepository conversationRepository;
    private final SearchIndexer searchIndexer;
    private final DomainObjectConverter converter;
    private final GumshieldApi gumshieldApi;
    private final ConversationEventListeners conversationEventListeners;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int maxConversationAgeDays;

    @Autowired
    public ConversationThreadController(
            MutableConversationRepository conversationRepository,
            PostBoxRepository postBoxRepository,
            SearchIndexer searchIndexer,
            DomainObjectConverter converter,
            GumshieldApi gumshieldApi,
            ConversationEventListeners conversationEventListeners) {
        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
        this.searchIndexer = searchIndexer;
        this.converter = converter;
        this.gumshieldApi = gumshieldApi;
        this.conversationEventListeners = conversationEventListeners;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterReportConversationCommand.MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.POST)
    @ResponseBody
    ResponseObject<?> reportConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return entityNotFound(response);
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, AT_POSTFIX, now.toString()));
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation)conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        markConversationAsRead(email, conversationId);

        ConversationRts conversationRts = converter.convertConversation(conversation);

        ApiFlaggedConversation flaggedConversation = new ApiFlaggedConversation();
        flaggedConversation.setAdvertId(Long.valueOf(conversationRts.getAdId()));
        flaggedConversation.setConversationId(conversationRts.getId());
        flaggedConversation.setReportedByEmail(email);
        flaggedConversation.setReportedDate(DateTime.parse(getFlaggedTime(conversationRts, email)));
        flaggedConversation.setReportedForEmail(getOtherEmail(conversationRts, email));
        gumshieldApi.conversationApi().flagConversation(flaggedConversation);
        return new ResponseObject();
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

    @RequestMapping(value = MessageCenterDeleteConversationCommand.MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<?> deleteConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return entityNotFound(response);
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, AT_POSTFIX, now.toString()));
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation)conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        markConversationAsRead(email, conversationId);

        ConversationRts conversationRts = converter.convertConversation(conversation);
        return ResponseObject.of(conversationRts);
    }


    private void markConversation(MutableConversation conversation, ConversationCustomValue customValue) {
        conversation.applyCommand(
                new AddCustomValueCommand(conversation.getId(), customValue.keyName(), customValue.value())
        );
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

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<?> getPostBoxConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletRequest request,
            HttpServletResponse response) {

        Timer.Context timerContext = API_POSTBOX_CONVERSATION_BY_ID.time();

        try {
            PostBox postBox = postBoxRepository.byId(email);

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound(response);
            }

            boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();
            if (needToMarkAsRead) {
                postBox.decrementNewReplies();
                postBoxRepository.write(postBox);
            }

            if (newCounterMode) {
                if (needToMarkAsRead) {
                    markConversationAsRead(email, conversationId, postBox);
                }
                return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, response);
            } else {
                long numUnread;
                if (needToMarkAsRead) {
                    numUnread = markConversationAsRead(email, conversationId, postBox);
                } else {
                    numUnread = postBox.getUnreadConversationsCapped().size();
                }
                return lookupConversation(numUnread, email, conversationId, response);
            }

        } finally {
            timerContext.stop();
        }
    }

    private ResponseObject<?> lookupConversation(long numUnread, String email, String conversationId, HttpServletResponse response) {

        Timer.Context riakTimerContext = API_POSTBOX_CONVERSATION_BY_ID_RIAK.time();
        Conversation conversation = null;
        try {
            conversation = conversationRepository.getById(conversationId);
        } finally {
            riakTimerContext.stop();
        }

        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket",
                    conversationId);
            return entityNotFound(response);
        }

        Timer.Context msgProcTimerContext = API_POSTBOX_CONVERSATION_BY_ID_MSG_PROCESSING.time();
        Optional<PostBoxSingleConversationThreadResponse> created;
        try {
            created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation);
        } finally {
            msgProcTimerContext.stop();
        }

        if (created.isPresent()) {
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
            return ResponseObject.of(created.get());
        } else {
            LOGGER.info(
                    "Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI",
                    conversationId);
            return entityNotFound(response);
        }

    }

    private long markConversationAsRead(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(email);
        return markConversationAsRead(email, conversationId, postBox);
    }

    private long markConversationAsRead(String email, String conversationId, PostBox postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<ConversationThread>();

        boolean needsUpdate = false;
        for (ConversationThread item : postBox.getConversationThreads()) {

            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {

                threadsToUpdate.add(new ConversationThread(
                        item.getAdId(),
                        item.getConversationId(),
                        item.getCreatedAt(),
                        now(),
                        item.getReceivedAt(),
                        false, // mark as read
                        item.getPreviewLastMessage(),
                        item.getBuyerName(),
                        item.getSellerName(),
                        item.getBuyerId(),
                        item.getSellerId(),
                        item.getMessageDirection()));

                needsUpdate = true;

            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox postBoxToUpdate = new PostBox.PostBoxBuilder().
                    withEmail(email).
                    withNewRepliesCounter(postBox.getNewRepliesCounter().getValue()).
                    withConversationThreads(threadsToUpdate).
                    withMaxConversationAgeDays(maxConversationAgeDays).
                    build();
            postBoxRepository.write(postBoxToUpdate);
            numUnreadCounter = postBoxToUpdate.getUnreadConversationsCapped().size();
        } else {
            numUnreadCounter = postBox.getUnreadConversationsCapped().size();
        }

        return numUnreadCounter;
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}
