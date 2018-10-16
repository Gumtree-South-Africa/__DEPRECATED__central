package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.comaas.events.Conversation;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedAndDeletedForUserCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.util.ConversationEventConverter;
import com.ecg.replyts.core.api.webapi.commands.GetConversationCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.AddCustomValuePayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.ChangeConversationStatePayload;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.indexer.DocumentSink;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static com.ecg.replyts.core.api.util.Constants.INTERRUPTED_WARNING;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * returns all informations, ReplyTS has for a specific conversation.
 */
@Controller
class ConversationController {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationController.class);

    private final MutableConversationRepository conversationRepository;
    private final DomainObjectConverter converter;
    private final MailCloakingService mailCloakingService;
    private final DocumentSink documentSink;
    private final ConversationEventListeners conversationEventListeners;
    private final UserIdentifierService userIdentifierService;
    private final Timer loadConversationTimer = TimingReports.newTimer("core-conversationController.loadConversation");
    private final ConversationEventService conversationEventService;

    @Value("${replyts.tenant.short:${replyts.tenant}}")
    private String shortTenant;

    @Autowired
    ConversationController(MutableConversationRepository conversationRepository,
                           DomainObjectConverter converter,
                           MailCloakingService mailCloakingService,
                           DocumentSink documentSink,
                           ConversationEventListeners conversationEventListeners,
                           UserIdentifierService userIdentifierService,
                           ConversationEventService conversationEventService) {
        this.conversationRepository = conversationRepository;
        this.converter = converter;
        this.mailCloakingService = mailCloakingService;
        this.documentSink = documentSink;
        this.conversationEventListeners = conversationEventListeners;
        this.userIdentifierService = userIdentifierService;
        this.conversationEventService = conversationEventService;
    }

    /**
     * return metadata and all messages for a conversation identified by the given ID.
     */
    @RequestMapping(value = GetConversationCommand.MAPPING, produces = APPLICATION_JSON_UTF8_VALUE, method = GET)
    @ResponseBody
    public ResponseObject<?> loadConversation(@PathVariable String conversationId) {
        try (Timer.Context ignored = loadConversationTimer.time()) {
            MutableConversation conversation = conversationRepository.getById(conversationId);
            ConversationRts conversationRts = converter.convertConversation(conversation);

            return (conversationRts == null) ?
                    ResponseObject.of(RequestState.ENTITY_NOT_FOUND, "Not found " + conversationId) :
                    ResponseObject.of(conversationRts);
        }
    }

    @RequestMapping(value = "/conversation/{conversationId}", produces = APPLICATION_JSON_UTF8_VALUE, method = PUT)
    @ResponseBody
    ResponseObject<?> closeConversation(@PathVariable String conversationId, @RequestBody ChangeConversationStatePayload changeConversationStatePayload) {
        Preconditions.checkArgument(ConversationState.CLOSED.equals(changeConversationStatePayload.getState()));

        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
        }

        checkThatIssuerIsBuyerOrSeller(changeConversationStatePayload, conversation);

        if (changeConversationStatePayload.isDeleteForIssuer()) {
            conversation.applyCommand(
                    new ConversationClosedAndDeletedForUserCommand(
                            conversationId,
                            changeConversationStatePayload.getIssuerId(),
                            changeConversationStatePayload.getIssuerEmail(),
                            DateTime.now(DateTimeZone.UTC)
                    )
            );
        } else {
            conversation.applyCommand(
                    new ConversationClosedCommand(
                            conversationId,
                            ConversationRole.getRole(changeConversationStatePayload.getIssuerEmail(), conversation),
                            DateTime.now(DateTimeZone.UTC)
                    )
            );
        }

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        Conversation.Participant.Role role = ConversationRole.getRole(changeConversationStatePayload.getIssuerEmail(), conversation).getParticipantRole();
        String emailSecret = role == Conversation.Participant.Role.BUYER ? conversation.getBuyerSecret() :
                role == Conversation.Participant.Role.SELLER ? conversation.getSellerSecret() : null;
        Conversation.Participant participant = ConversationEventConverter.createParticipant(changeConversationStatePayload.getIssuerId(), null, changeConversationStatePayload.getIssuerEmail(), role, emailSecret);

        return placeDeleteEventOnQueue(conversationId, participant);
    }

    private ResponseObject<?> placeDeleteEventOnQueue(String conversationId, Conversation.Participant participant) {
        try {
            conversationEventService.sendConversationDeletedEvent(shortTenant, conversationId, participant);
            return ResponseObject.of(RequestState.OK);
        } catch (InterruptedException e) {
            LOG.warn("Aborting conversation deleting because thread is interrupted. conversation id: {}. " + INTERRUPTED_WARNING, conversationId);
            Thread.currentThread().interrupt();
            return ResponseObject.of(RequestState.INTERNAL_SERVER_ERROR, "Thread was interrupted.");
        }
    }

    @RequestMapping(value = "/conversation/{conversationId}/customValue", method = PUT)
    @ResponseBody
    ResponseObject<?> addCustomValue(@PathVariable("conversationId") String convId, @RequestBody AddCustomValuePayload cmd) {
        MutableConversation byId = conversationRepository.getById(convId);
        byId.applyCommand(new AddCustomValueCommand(convId, cmd.getKey(), cmd.getValue()));

        ((DefaultMutableConversation) byId).commit(conversationRepository, conversationEventListeners);
        documentSink.sink(byId);

        return ResponseObject.of(converter.convertConversation(byId));
    }

    @RequestMapping(value = "conversation/bymail", produces = APPLICATION_JSON_UTF8_VALUE, method = GET)
    @ResponseBody
    ResponseObject<?> loadConversationByMail(@RequestParam("mail") String mailAddress) {
        MailAddress mail = new MailAddress(mailAddress);

        Optional<CloakedReceiverContext> receiverContext = mailCloakingService.resolveUser(mail);
        if (!receiverContext.isPresent()) {
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND, "Not found " + mailAddress);
        }
        ConversationRts conversationRts = converter.convertConversation(receiverContext.get().getConversation());
        return ResponseObject.of(conversationRts);
    }

    private void checkThatIssuerIsBuyerOrSeller(ChangeConversationStatePayload changeConversationStatePayload, MutableConversation conversation) {
        String issuerId = changeConversationStatePayload.getIssuerId();
        String issuerEmail = changeConversationStatePayload.getIssuerEmail();

        String buyerId = userIdentifierService.getBuyerUserId(conversation).orElse("");
        String sellerId = userIdentifierService.getSellerUserId(conversation).orElse("");

        String buyerEmail = conversation.getBuyerId();
        String sellerEmail = conversation.getSellerId();

        Preconditions.checkArgument(
                issuerId.equalsIgnoreCase(buyerId) || issuerId.equalsIgnoreCase(sellerId) ||
                        issuerEmail.equalsIgnoreCase(buyerEmail) || issuerEmail.equalsIgnoreCase(sellerEmail),
                "issuerId '%s' or issuerEmail '%s' are not one of: buyerId '%s',  sellerId '%s', buyerEmail '%s' or sellerEmail '%s'",
                issuerId, issuerEmail,
                buyerId, sellerId,
                buyerEmail, sellerEmail);
    }
}
