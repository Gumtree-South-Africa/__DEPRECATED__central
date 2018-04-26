package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.kjca.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.webapi.requests.MessageCenterGetAdConversationRecipientsCommand;
import com.ecg.messagecenter.kjca.webapi.responses.AdConversationRecipientListResponse;
import com.ecg.messagecenter.kjca.webapi.responses.BuyerContactResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Used to allow system to get contact details of people who've contacted a seller about an ad.
 * We use these details to notify them that an ad is being deleted.
 */
@Controller
@Deprecated
class AdConversationRecipientsController {

    private final SimplePostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public AdConversationRecipientsController(
            ConversationRepository conversationRepository,
            SimplePostBoxRepository postBoxRepository) {

        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }


    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    /**
     * Get contact details of people who've replied to an ad.
     *
     * WARNING: Clients should encode the seller email TWICE.
     * Spring actually performs url decoding when it parses the @PathVariable.
     * But encoding seller email only once results in a 404 for an emails that
     * have the "/" character. It also results in problems for emails with the "+" character, because url decoding twice ends up
     * removing the "+" from the email ("name+suffix@domain.com" becomes "name suffix@domain.com".
     * Decoding twice doesn't break more standard format emails.
     *
     * @param urlEncodedSellerEmail we use urlEncoded email because path separators "/" are also valid email characters, and this results in the URL not getting mapped to this controller
     * @param adId the ad for which we want buyers
     * @param state (optional) the state of our conversation. defaults to "ACTIVE". normally left blank as we will almost always just want buyers with ACTIVE conversations.
     * @return object containing buyer details.
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = MessageCenterGetAdConversationRecipientsCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    @ResponseBody
    ResponseObject<?> getBuyerContactsForAd(
            @PathVariable("urlEncodedSellerEmail") String urlEncodedSellerEmail,
            @PathVariable("adId") String adId,
            @RequestParam(value = "state", defaultValue = "ACTIVE", required = false) ConversationState state) throws UnsupportedEncodingException {

        final String sellerEmail = URLDecoder.decode(urlEncodedSellerEmail, StandardCharsets.UTF_8.name());
        final PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(sellerEmail));

        final List<BuyerContactResponse> buyerContacts = new ArrayList<>();

        final AdConversationRecipientListResponse adConversationRecipientListResponse = new AdConversationRecipientListResponse(adId, buyerContacts);

        for (ConversationThread conversationThread : postBox.getConversationThreads()) {

            // only look at conversations for given ad
            if (conversationThread.getAdId().equals(adId)) {
                // that match the state (default = ACTIVE)
                final Conversation conversation = conversationRepository.getById(conversationThread.getConversationId());
                if (conversation != null && conversation.getState().equals(state)) {
                    addBuyerContact(buyerContacts, conversationThread, conversation.getState());
                }
            }
        }

        return ResponseObject.of(adConversationRecipientListResponse);
    }

    private void addBuyerContact(List<BuyerContactResponse> buyerContacts, ConversationThread conversationThread, ConversationState state) {
        final BuyerContactResponse buyerContactResponse =
                new BuyerContactResponse(conversationThread.getBuyerName().orElse(""),
                        conversationThread.getBuyerId().get(),
                        conversationThread.getConversationId(),
                        conversationThread.getModifiedAt(),
                        state);

        buyerContacts.add(buyerContactResponse);
    }

}
