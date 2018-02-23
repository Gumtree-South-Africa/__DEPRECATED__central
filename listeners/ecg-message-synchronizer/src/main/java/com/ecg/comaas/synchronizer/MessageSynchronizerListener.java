package com.ecg.comaas.synchronizer;

import com.ecg.comaas.synchronizer.extractor.SyncMessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Synchronizer works as connector between two Comaas tenants. If one Comaas is processing an email and finds
 * {@link MessageSynchronizerListener#PARTNER_ADID} header then this email was sent on behalf of any partner specified using header
 * {@link MessageSynchronizerListener#PARTNER_TENANT}. The synchronizer then sends an HTTP message to partner's comaas with and actual
 * message and partner's information to store this message using its own MessageBox instance.
 *
 * Messages are then synchronized between tenant's comaas instances.
 */
@Component
@Order(value = 510)
@ConditionalOnProperty(name = "message.synchronizer.enabled", havingValue = "true")
public class MessageSynchronizerListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(MessageSynchronizerListener.class);

    private static final String PARTNER_ADID = "partner-adid";
    private static final String PARTNER_FROM_USERID = "partner-from-userid";
    private static final String PARTNER_TO_USERID = "partner-to-userid";
    private static final String PARTNER_BUYER_NAME = "partner-buyer-name";
    private static final String PARTNER_SELLER_NAME = "partner-seller-name";
    private static final String PARTNER_TENANT = "partner-tenant";
    private static final String PARTNER_TITLE = "partner-title";

    private static final String MSGBOX_PARTNER_SYNC_PATH = "msgcenter/partner-sync";

    private final SyncMessagesResponseFactory messagesResponseFactory;
    private final PartnerConfiguration partnerConfiguration;
    private final JerseyClient partnerClient;

    @Autowired
    public MessageSynchronizerListener(SyncMessagesResponseFactory messagesResponseFactory, PartnerConfiguration partnerConfiguration) {
        this.messagesResponseFactory = messagesResponseFactory;
        this.partnerConfiguration = partnerConfiguration;
        this.partnerClient = JerseyClientBuilder.createClient()
                .register(new JacksonJaxbJsonProvider());
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        boolean isPartnerConversation = conversation.getCustomValues().containsKey(PARTNER_ADID);

        if (isPartnerConversation) {
            processMessageInternal(conversation, message);
        }
    }

    private void processMessageInternal(Conversation conversation, Message message) {
        String partnerFromUserid = extractProperty(conversation, PARTNER_FROM_USERID);
        String partnerToUserid = extractProperty(conversation, PARTNER_TO_USERID);
        String partnerBuyerName = extractProperty(conversation, PARTNER_BUYER_NAME);
        String partnerSellerName = extractProperty(conversation, PARTNER_SELLER_NAME);
        String partnerAdId = extractProperty(conversation, PARTNER_ADID);
        String partnerTenant = extractProperty(conversation, PARTNER_TENANT);
        String partnerTitle = extractProperty(conversation, PARTNER_TITLE);

        ObjectNode payload = ObjectMapperConfigurer.objectBuilder()
                .put("text", messagesResponseFactory.getCleanedMessage(conversation, message))
                .put("senderUserId", partnerFromUserid)
                .put("type", "partner")
                .put("adId", partnerAdId)
                .put("adTitle", partnerTitle)
                .put("subject", message.getHeaders().get("Subject"));

        payload.set("buyer", createParticipant(partnerFromUserid, partnerBuyerName, "buyer"));
        payload.set("seller", createParticipant(partnerToUserid, partnerSellerName, "seller"));

        String partnerAddress = partnerConfiguration.getAddress(partnerTenant);
        if (StringUtils.isBlank(partnerAddress)) {
            throw new RuntimeException(String.format("Partner address is not configured: %s", partnerAddress));
        }

        Response response = partnerClient
                .target(partnerAddress)
                .path(MSGBOX_PARTNER_SYNC_PATH)
                .request()
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON_TYPE));

        LOG.debug("Partner message sent. Partner Tenant: {}, Partner From-UserId: {}, Partner To-UserId: {}, Partner AdId: {}, Result Code: {}",
                partnerTenant, partnerFromUserid, partnerToUserid, partnerAdId, response.getStatus());
    }

    private static ObjectNode createParticipant(String userId, String userName, String participant) {
        return ObjectMapperConfigurer.objectBuilder()
                .put("userId", userId)
                .put("name", userName)
                .put("role", participant);
    }

    private static String extractProperty(Conversation conversation, String propertyName) {
        checkArgument(conversation.getCustomValues().containsKey(propertyName),
                "Partner Property '%s' is missing. Partner's message synchronization is skipped.", propertyName);
        return conversation.getCustomValues().get(propertyName);
    }
}