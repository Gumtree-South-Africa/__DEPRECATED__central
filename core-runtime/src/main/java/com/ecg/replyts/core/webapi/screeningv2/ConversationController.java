package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationClosedCommand;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.webapi.commands.GetConversationCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.AddCustomValuePayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.ChangeConversationStatePayload;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * returns all informations, ReplyTS has for a specific conversation.
 */
@Controller
class ConversationController {

    private final MutableConversationRepository conversationRepository;
    private final DomainObjectConverter converter;
    private final MailCloakingService mailCloakingService;
    private final SearchIndexer searchIndexer;
    private final ConversationEventListeners conversationEventListeners;
    private final Timer loadConversationTimer = TimingReports.newTimer("core-conversationController.loadConversation");

    @Autowired
    ConversationController(MutableConversationRepository conversationRepository,
                           DomainObjectConverter converter,
                           MailCloakingService mailCloakingService,
                           SearchIndexer searchIndexer,
                           ConversationEventListeners conversationEventListeners) {
        this.conversationRepository = conversationRepository;
        this.converter = converter;
        this.mailCloakingService = mailCloakingService;
        this.searchIndexer = searchIndexer;
        this.conversationEventListeners = conversationEventListeners;
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

        PreconditionIssuerEmailIsBuyerOrSeller(changeConversationStatePayload, conversation);

        conversation.applyCommand(
                new ConversationClosedCommand(
                    conversationId,
                    ConversationRole.getRole(changeConversationStatePayload.getIssuerEmail(), conversation),
                    DateTime.now()
                )
        );

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        return ResponseObject.of(RequestState.OK);
    }

    @RequestMapping(value="/conversation/{conversationId}/customValue", method = PUT)
    @ResponseBody
    ResponseObject<?> addCustomValue(@PathVariable("conversationId") String convId, @RequestBody AddCustomValuePayload cmd) {
        MutableConversation byId = conversationRepository.getById(convId);
        byId.applyCommand(new AddCustomValueCommand(convId, cmd.getKey(), cmd.getValue()));

        ((DefaultMutableConversation)byId).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(byId));

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

    private void PreconditionIssuerEmailIsBuyerOrSeller(ChangeConversationStatePayload changeConversationStatePayload, MutableConversation conversation) {
        String issuerEmail = changeConversationStatePayload.getIssuerEmail();
        String buyerId = conversation.getBuyerId();
        String sellerId = conversation.getSellerId();
        Preconditions.checkArgument(
            issuerEmail.equalsIgnoreCase(buyerId) || issuerEmail.equalsIgnoreCase(sellerId),
            "issuerEmail {} is not buyerId {} or sellerId {}",
            issuerEmail,
            buyerId,
            sellerId);
    }
}
