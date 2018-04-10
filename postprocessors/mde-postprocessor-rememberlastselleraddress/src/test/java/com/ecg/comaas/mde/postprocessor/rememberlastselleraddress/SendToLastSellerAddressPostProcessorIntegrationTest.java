package com.ecg.comaas.mde.postprocessor.rememberlastselleraddress;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;


public class SendToLastSellerAddressPostProcessorIntegrationTest {

    private final static String DOMAIN = "@test-platform.com";

    private final static String BUYER = "anton@privat.com";

    private final static String BUYER_WITH_FIRST_NAME = "Anton <" + BUYER + ">";

    private final static String SELLER_COMMON = "My Company <info@company.com>";

    private final static String SELLER = "bob@company.com";

    private final static String SELLER_WITH_FIRST_NAME = "Bob <" + SELLER + ">";

    private final static String SELLER_WITH_FULL_NAME = "Bob Foobar <" + SELLER + ">";

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule();

    @Test
    public void doesNotTouchBuyerEmailAddresses() {
        ProcessedMail buyerMessage1 = messageFrom(BUYER).to(SELLER_COMMON);

        Anonymizer anonymizer = new Anonymizer(buyerMessage1);

        messageFrom(SELLER_WITH_FIRST_NAME).to(anonymizer.buyer);
        messageFrom("Anton <" + "other@address.com" + ">").to(anonymizer.seller);

        ProcessedMail sellerMessage2 = messageFrom(SELLER_WITH_FIRST_NAME).to(anonymizer.buyer);

        assertEquals(4, sellerMessage2.getConversation().getMessages().size());
        assertEquals(BUYER, sellerMessage2.getOutboundMail().getTo().get(0));

    }

    @Test
    public void extractsSellerMailAddress() throws Exception {

        ProcessedMail buyerMessage1 = messageFrom(BUYER).to(SELLER_COMMON);

        Anonymizer anonymizer = new Anonymizer(buyerMessage1);

        messageFrom(SELLER_WITH_FIRST_NAME).to(anonymizer.buyer);

        ProcessedMail buyerMessage2 = messageFrom(BUYER_WITH_FIRST_NAME).to(anonymizer.seller);

        assertEquals(3, buyerMessage2.getConversation().getMessages().size());
        assertEquals(SELLER, buyerMessage2.getOutboundMail().getTo().get(0));
    }

    @Test
    public void extractsSellerMailAddressFromDifferentHeader() throws Exception {

        ProcessedMail buyerMessage1 = messageFrom(BUYER).to(SELLER_COMMON);

        Anonymizer anonymizer = new Anonymizer(buyerMessage1);

        messageFrom(SELLER_WITH_FULL_NAME).to(anonymizer.buyer);

        ProcessedMail buyerMessage2 = messageFrom(BUYER_WITH_FIRST_NAME).to(anonymizer.seller);

        assertEquals(3, buyerMessage2.getConversation().getMessages().size());
        assertEquals(SELLER, buyerMessage2.getOutboundMail().getTo().get(0));
    }

    @Test
    public void usesLastSellerMailAddress() throws Exception {

        ProcessedMail buyerMessage1 = messageFrom(BUYER).to(SELLER_COMMON);

        Anonymizer anonymizer = new Anonymizer(buyerMessage1);

        messageFrom("Other <" + "other@address.com" + ">").to(anonymizer.buyer);

        messageFrom(SELLER_WITH_FIRST_NAME).to(anonymizer.buyer);

        ProcessedMail buyerMessage2 = messageFrom(BUYER_WITH_FIRST_NAME).to(anonymizer.seller);

        assertEquals(SELLER, buyerMessage2.getOutboundMail().getTo().get(0));
    }

    @Test
    public void doesNothingIfSellerAddressNotAvailable() throws Exception {

        ProcessedMail buyerMessage1 = messageFrom(BUYER).to(SELLER_COMMON);

        Anonymizer anonymizer = new Anonymizer(buyerMessage1);

        ProcessedMail buyerMessage2 = messageFrom(BUYER_WITH_FIRST_NAME).to(anonymizer.seller);

        assertEquals("info@company.com", buyerMessage2.getOutboundMail().getTo().get(0));
    }

    private class Anonymizer {

        private final String buyer;

        private final String seller;

        private Anonymizer(ProcessedMail processedMail) {
            buyer = new StringBuilder("Buyer.")
                    .append(processedMail.getConversation().getSecretFor(ConversationRole.Buyer)).append(DOMAIN)
                    .toString();
            seller = new StringBuilder("Seller.")
                    .append(processedMail.getConversation().getSecretFor(ConversationRole.Seller)).append(DOMAIN)
                    .toString();
        }

    }

    private class MailBuilderWrapper {
        private MailBuilderWrapper(MailBuilder mailBuilder) {
            this.mailBuilder = mailBuilder;
        }

        private final MailBuilder mailBuilder;

        private ProcessedMail to(String to) {
            mailBuilder.to(to);
            return replyTsIntegrationTestRule.deliver(mailBuilder);
        }
    }

    private MailBuilderWrapper messageFrom(String from) {
        return new MailBuilderWrapper(aNewMail().adId("1234").from(from).htmlBody("Bla Bla"));

    }

}
