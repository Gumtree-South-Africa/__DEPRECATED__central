package com.ecg.de.ebayk.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.persistence.ConversationThread;
import com.ecg.de.ebayk.messagecenter.persistence.PostBox;
import com.ecg.de.ebayk.messagecenter.persistence.PostBoxRepository;
import com.ecg.de.ebayk.messagecenter.webapi.requests.MessageCenterGetAdConversationRecipientsCommand;
import com.ecg.de.ebayk.messagecenter.webapi.responses.AdConversationRecipientListResponse;
import com.ecg.de.ebayk.messagecenter.webapi.responses.BuyerContactResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Used to allow system to get contact details of people who've contacted a seller about an ad.
 * We use these details to notify them that an ad is being deleted.
 */
@Controller class AdConversationRecipientsController {

    private static final Logger LOGGER =
                    LoggerFactory.getLogger(AdConversationRecipientsController.class);

    private static final Timer API_POSTBOX_ENQUIRY_BY_AD =
                    TimingReports.newTimer("webapi-postbox-enquiry-by-ad");
    private static final Histogram API_NUM_CONVERSATIONS_BY_AD =
                    TimingReports.newHistogram("webapi-postbox-num-conversations-by-ad");


    private final PostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public AdConversationRecipientsController(ConversationRepository conversationRepository,
                    PostBoxRepository postBoxRepository) {

        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
    }


    @InitBinder public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer)
                    throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    /**
     * Get contact details of people who've replied to an ad.
     * <p>
     * WARNING: Clients should encode the seller email TWICE.
     * Spring actually performs url decoding when it parses the @PathVariable.
     * But encoding seller email only once results in a 404 for an emails that
     * have the "/" character. It also results in problems for emails with the "+" character, because url decoding twice ends up
     * removing the "+" from the email ("name+suffix@domain.com" becomes "name suffix@domain.com".
     * Decoding twice doesn't break more standard format emails.
     *
     * @param urlEncodedSellerEmail we use urlEncoded email because path separators "/" are also valid email characters, and this results in the URL not getting mapped to this controller
     * @param adId                  the ad for which we want buyers
     * @param state                 (optional) the state of our conversation. defaults to "ACTIVE". normally left blank as we will almost always just want buyers with ACTIVE conversations.
     * @return object containing buyer details.
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = MessageCenterGetAdConversationRecipientsCommand.MAPPING,
                    produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody ResponseObject<?> getBuyerContactsForAd(
                    @PathVariable("urlEncodedSellerEmail") String urlEncodedSellerEmail,
                    @PathVariable("adId") String adId,
                    @RequestParam(value = "state", defaultValue = "ACTIVE", required = false)
                    ConversationState state) throws UnsupportedEncodingException {

        Timer.Context timerContext = API_POSTBOX_ENQUIRY_BY_AD.time();

        try {
            final String sellerEmail =
                            URLDecoder.decode(urlEncodedSellerEmail, StandardCharsets.UTF_8.name());
            final PostBox postBox = postBoxRepository.byId(sellerEmail);

            final List<BuyerContactResponse> buyerContacts = new ArrayList<>();

            final AdConversationRecipientListResponse adConversationRecipientListResponse =
                            new AdConversationRecipientListResponse(adId, buyerContacts);

            for (ConversationThread conversationThread : postBox.getConversationThreads()) {

                try {
                    // only look at conversations for given ad
                    if (conversationThread.getAdId().equals(adId)) {
                        // that match the state (default = ACTIVE)
                        final Conversation conversation = conversationRepository
                                        .getById(conversationThread.getConversationId());
                        if (conversation.getState().equals(state)) {
                            addBuyerContact(buyerContacts, conversationThread,
                                            conversation.getState());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while processing conversation in loop", e);
                }
            }

            API_NUM_CONVERSATIONS_BY_AD.update(buyerContacts.size());

            return ResponseObject.of(adConversationRecipientListResponse);

        } catch (RuntimeException e) {
            LOGGER.error("Exception while processing conversation", e);
            throw e;
        } finally {
            timerContext.stop();
        }
    }

    private void addBuyerContact(List<BuyerContactResponse> buyerContacts,
                    ConversationThread conversationThread, ConversationState state) {
        final BuyerContactResponse buyerContactResponse =
                        new BuyerContactResponse(conversationThread.getBuyerName().or(""),
                                        conversationThread.getBuyerId().get(),
                                        conversationThread.getConversationId(),
                                        conversationThread.getModifiedAt(), state);

        buyerContacts.add(buyerContactResponse);
    }

}
