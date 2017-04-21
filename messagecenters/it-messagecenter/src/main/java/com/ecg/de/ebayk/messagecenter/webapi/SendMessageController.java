package com.ecg.de.ebayk.messagecenter.webapi;

import com.ecg.de.ebayk.messagecenter.chat.Template;
import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.de.ebayk.messagecenter.util.AdUtil;
import com.ecg.de.ebayk.messagecenter.webapi.requests.ConversationContentPayload;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterSendMessageCommand;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterStartConversationCommand;
import com.ecg.de.ebayk.messagecenter.webapi.requests.StartConversationContentPayload;
import com.ecg.de.ebayk.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.de.ebayk.messagecenter.webapi.responses.ResponseUtil;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.StringUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Created by jaludden on 20/11/15.
 */
@Controller public class SendMessageController {

    private final Template template;
    private final String adIdPrefix;
    private MessageProcessingCoordinator coordinator;
    private ConversationRepository conversationRepository;
    private PostBoxRepository postBoxRepository;
    private MailCloakingService mailCloakingService;

    @Autowired public SendMessageController(MessageProcessingCoordinator coordinator,
                    ConversationRepository conversationRepository, Template template,
                    PostBoxRepository postBoxRepository, MailCloakingService mailCloakingService,
                    @Value("${api.adIdPrefix:}") String adIdPrefix) {
        this.coordinator = coordinator;
        this.conversationRepository = conversationRepository;
        this.template = template;
        this.postBoxRepository = postBoxRepository;
        this.mailCloakingService = mailCloakingService;
        this.adIdPrefix = adIdPrefix;
    }

    @RequestMapping(value = MessageCenterStartConversationCommand.MAPPING, method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody public ResponseObject<?> createConversation(@PathVariable("email") String email,
                    @RequestBody StartConversationContentPayload payload,
                    HttpServletResponse response) throws IOException {
        Address from = new Address(payload.getBuyerName(), email);
        Address to = new Address(payload.getSellerName(), payload.getSellerEmail());
        payload.cleanupMessage();
        payload.setMessage(createTemplatedMessage(payload, from.getName()));

        DateTime startTime = DateTime.now();

        sendMessage(payload.getAdId(), payload.getMessage(), from, to, from.getName(),
                        payload.getSubject());

        return createResponse(email, getConversation(payload.getAdId(), from.getEmail(), startTime,
                        payload.getSellerEmail()), response);
    }

    private String createTemplatedMessage(StartConversationContentPayload payload, String from) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("from", from);
        variables.put("message", payload.getMessage());
        variables.put("title", payload.getAdTitle());
        variables.put("ad_id", payload.getAdId());
        variables.put("type", payload.getType());
        variables.put("greating", payload.getGreating());
        variables.put("toSeller", true);
        if (ResponseUtil.hasName(from)) {
            variables.put("emailNickname", from);
        }
        return template.createPostReplyMessage(variables);
    }

    @RequestMapping(value = MessageCenterSendMessageCommand.MAPPING, method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ResponseBody public ResponseObject<?> replyConversation(@PathVariable("email") String email,
                    @PathVariable("conversationId") String conversationId,
                    @RequestBody ConversationContentPayload payload, HttpServletResponse response)
                    throws IOException {
        PostBox postBox = postBoxRepository.byId(email);
        Optional<ConversationThread> lookupResult = postBox.lookupConversation(conversationId);
        if (!lookupResult.isPresent()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return ResponseObject.of(RequestState.ENTITY_NOT_FOUND, "Not found");
        }

        Address from = new Address(payload.getUsername(), email);
        Conversation conversation = conversationRepository.getById(conversationId);
        ConversationRole role = ConversationRole.getRole(email, conversation);
        Address to = getToAddress(role, conversation, lookupResult.get());
        String title = getTitle(conversation);
        payload.cleanupMessage();
        payload.setMessage(createTemplatedMessage(payload, from.getName(), conversation, role));
        sendMessage(payload.getAdId(), payload.getMessage(), from, to, "", "Re: " + title);

        return createResponse(email,
                        Optional.fromNullable(conversationRepository.getById(conversationId)),
                        response);
    }

    private ResponseObject<?> createResponse(@PathVariable("email") String email,
                    Optional<Conversation> conversationAfterUpdate, HttpServletResponse response) {
        Optional<PostBoxSingleConversationThreadResponse> r;
        if (conversationAfterUpdate.isPresent()) {
            r = PostBoxSingleConversationThreadResponse
                            .create(0, email, conversationAfterUpdate.get(), true);
        } else {
            r = Optional.absent();
        }

        if (r.isPresent()) {
            response.setStatus(HttpStatus.CREATED.value());
            return ResponseObject.of(r.get());
        }
        response.setStatus(HttpStatus.CREATED.value());
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND, "Not found");
    }

    private String getTitle(Conversation conversation) {
        if (conversation.getMessages().isEmpty()) {
            return "";
        }
        String title = conversation.getMessages().get(0).getHeaders().get("Subject");
        if (StringUtil.isBlank(title)) {
            return "";
        }
        return title;
    }

    private String createTemplatedMessage(ConversationContentPayload payload, String from,
                    Conversation conversation, ConversationRole role) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("from", from);
        variables.put("message", payload.getMessage());
        variables.put("ad_id", payload.getAdId());
        variables.put("type", payload.getType());
        variables.put("greating", payload.getGreating());
        variables.put("toSeller", role == ConversationRole.Buyer);

        return template.createPostReplyMessage(variables);
    }

    private Address getToAddress(ConversationRole role, Conversation conversation,
                    ConversationThread thread) {
        String email = mailCloakingService
                        .createdCloakedMailAddress(getOtherRole(role), conversation).getAddress();
        String name = (role == ConversationRole.Seller) ?
                        thread.getBuyerName().orNull() :
                        thread.getSellerName().orNull();
        return new Address(name, email);
    }

    private ConversationRole getOtherRole(ConversationRole role) {
        return role == ConversationRole.Buyer ? ConversationRole.Seller : ConversationRole.Buyer;
    }

    private void sendMessage(@RequestParam("adid") Long adId,
                    @RequestParam("message") String message, Address from, Address to,
                    String buyerName, String subject) throws IOException {
        ConversationMessage conversationMessage =
                        new ConversationMessage(adId, from, to, message, buyerName, subject);
        coordinator.accept(conversationMessage.asInputStream());
    }

    private class Address {
        private String name;
        private String email;

        public Address(String name, String email) {
            this.name = StringUtils.isBlank(name) ? "Utente di Kijiji" : name;
            this.email = email;
        }

        public Address(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            if (StringUtils.isEmpty(name)) {
                return email;
            }
            return name + " <" + email + ">";
        }
    }

    private Optional<Conversation> getConversation(long adId, String email, DateTime startTime,
                    String sellerEmail) {
        PostBox postBox = postBoxRepository.byId(email);

        return postBox.filter(modifiedAfter(startTime)).filter(onAdId(adId))
                        .transform(toConversation()).firstMatch(onSellerEmail(sellerEmail));
    }

    private Predicate<? super Conversation> onSellerEmail(String sellerEmail) {
        return new Predicate<Conversation>() {
            @Override public boolean apply(@Nullable Conversation conversation) {
                return conversation.getSellerId().equals(sellerEmail);
            }
        };
    }

    private Function<? super ConversationThread, Conversation> toConversation() {
        return new Function<ConversationThread, Conversation>() {
            @Nullable @Override
            public Conversation apply(@Nullable ConversationThread conversationThread) {
                return conversationRepository.getById(conversationThread.getConversationId());
            }
        };
    }

    private Predicate<? super ConversationThread> modifiedAfter(DateTime time) {
        return new Predicate<ConversationThread>() {
            @Override public boolean apply(ConversationThread conversationThread) {
                return conversationThread.getModifiedAt().isAfter(time);
            }
        };
    }

    private Predicate<? super ConversationThread> onAdId(long adId) {
        return new Predicate<ConversationThread>() {
            @Override public boolean apply(ConversationThread conversationThread) {
                return AdUtil.getAdFromMail(conversationThread.getAdId(), adIdPrefix) == adId;
            }
        };
    }

    private class ConversationMessage {

        private final Address to;
        private long adId;

        private final Address from;

        private final String message;
        private String buyerName;
        private String subject;

        public ConversationMessage(long adId, Address from, Address to, String message,
                        String buyerName, String subject) {
            this.adId = adId;
            this.from = from;
            this.to = to;
            this.message = message;
            this.buyerName = buyerName;
            this.subject = subject;
        }

        public InputStream asInputStream() throws IOException {
            return new ByteArrayInputStream(getMailMessage().getBytes("UTF-8"));
        }

        public String getMailMessage() {
            Map<String, String> headers = new LinkedHashMap<>();

            headers.put("TO", getTo());
            headers.put("FROM", getFrom());
            headers.put("DATE", getDate());
            headers.put("SUBJECT", getSubject());
            headers.put("Content-type", "text/html; charset=UTF-8");

            headers.put("X-ORIGINAL-TO", getOriginalTo());
            headers.put("DELIVERED-TO", getDeliveredTo());

            headers.put("X-ADID", "ad_" + getAdId());
            headers.put("X-CUST-BUYER-NAME", from.getName());
            headers.put("X-CUST-SELLER-NAME", to.getName());

            String headerString = "";
            for (Map.Entry<String, String> h : headers.entrySet()) {
                headerString += h.getKey() + ": " + h.getValue() + "\n";
            }

            return headerString + "\n" + getMessage() + "\n";
        }

        private String getBuyerName() {
            return buyerName;
        }

        private String getMessage() {
            return message;
        }

        private long getAdId() {
            return adId;
        }

        private String getFrom() {
            return from.getFullName();
        }

        private String getSubject() {
            return subject;
        }

        private String getTo() {
            return to.getFullName();
        }

        private String getDeliveredTo() {
            return getTo();
        }

        private String getOriginalTo() {
            return getTo();
        }

        private String getDate() {
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
            return format.format(new Date());
        }
    }
}
