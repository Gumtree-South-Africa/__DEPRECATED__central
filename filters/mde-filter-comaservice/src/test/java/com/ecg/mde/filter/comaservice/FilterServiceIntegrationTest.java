package com.ecg.mde.filter.comaservice;

import com.ecg.mde.filter.comaservice.filters.ContactMessage;
import com.ecg.mde.filter.comaservice.filters.PhoneNumber;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FilterServiceIntegrationTest {

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(createProperties());

    private Properties createProperties() {
        Properties properties = new Properties();
        properties.put("replyts.mobile.comafilterservice.webserviceUrl","http://localhost:8181");
        properties.put("replyts.mobile.comafilterservice.active","true");
        return properties;
    }

    private Date testStartTime;

    @BeforeClass
    public static void setup() throws Exception {
        createServer(MockServlet.class);
    }

    @Before
    public void setupTest() {
        MockServlet.reset();
        testStartTime = Instant.now().withMillis(0).toDate();
        replyTsIntegrationTestRule.registerConfig(FilterServiceFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse("{}"));
    }

    private static void createServer(Class<? extends HttpServlet> servlet) throws Exception {
        Server server = new Server(8181);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(servlet, "/*");
        server.start();
    }

    @Test
    public void testSendingMessage() throws IOException {
        MailBuilder mailBuilder = createDummyMailWithFields();
        MockServlet.setJsonResponse("[]");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());

        verifyReceivedContactMessageWithFields(MockServlet.getContactMessage());
    }

    @Test
    public void testBlockingMessage() throws IOException {
        MailBuilder mailBuilder = createDummyMailWithFields();
        MockServlet.setJsonResponse("[\"AnyFilter\"]");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);
        assertEquals(MessageState.BLOCKED, processedMail.getMessage().getState());

        verifyReceivedContactMessageWithFields(MockServlet.getContactMessage());
    }

    @Test
    public void testSendingMessageWithInvalidFields() throws IOException {
        MailBuilder mailBuilder = createDummyMailWithFields();
        mailBuilder.header("X-Cust-PHONE_NUMBER_COUNTRY_CODE", "A")
                .header("X-Cust-PHONE_NUMBER_DISPLAY_NUMBER", "B")
                .header("X-Cust-IP_ADDRESS_V4V6", "127.0.0.")
                .header("X-Cust-SELLER_SITE_ID", "0")

                .header("Date","Tue, 24 Feb 2015 14:08:15");
        MockServlet.setJsonResponse("[]");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());

        assertTrue(MockServlet.getContactMessage().isPresent());
    }

    @Test
    public void testNotFilteringMessageToDealerBuyer() throws IOException {
        MailBuilder mailBuilder = createDummyMailBase()
                .header("X-Cust-Seller_Type", "DEALER")
                .header("X-Cust-Buyer_Type", "DEALER");
        MockServlet.setJsonResponse("[\"AnyResponseThatShouldNeverBeAskedFor\"]");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());

        assertFalse(MockServlet.getContactMessage().isPresent());
    }


    @Test
    public void testSendingMessageWithoutOptionalFields() throws IOException {
        MailBuilder mailBuilder = createDummyMailBase()
                .header("X-Cust-Seller_Type", "DEALER");
        MockServlet.setJsonResponse("[]");

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());

        Optional<ContactMessage> contactMessage = MockServlet.getContactMessage();

        verifyReceivedContactMessageBase(contactMessage);
        verifyUndefinedMessageCreationDate(contactMessage);
        assertEquals(null, contactMessage.get().getBuyerPhoneNumber());
        assertEquals(null, contactMessage.get().getSiteId());
        assertEquals(null, contactMessage.get().getIpAddressV4V6());
    }


    private MailBuilder createDummyMailBase() {
        return MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo bar </html>")
                .plainBody("foo bar")
                .from("mobile.de <noreply@team.mobile.de>")
                .header("Reply-To", "buyer <buyer@team.mobile.de>")
                .to("Seller <seller@example.com>");
    }

    private MailBuilder createDummyMailWithFields() {
        return createDummyMailBase()
                .header("X-Cust-PHONE_NUMBER_COUNTRY_CODE", "49")
                .header("X-Cust-PHONE_NUMBER_DISPLAY_NUMBER", "123456")
                .header("X-Cust-IP_ADDRESS_V4V6", "127.0.0.1")
                .header("X-Cust-SELLER_SITE_ID", "GERMANY")

                .header("Date","Tue, 24 Feb 2015 14:08:15 +0000")

                .header("X-Cust-Seller_Type", "DEALER")
                .header("X-Cust-Buyer_Type", "OTHER");
    }

    private void verifyReceivedContactMessageBase(Optional<ContactMessage> contactMessage) {

        assertTrue(contactMessage.isPresent());
        assertEquals("foo bar", contactMessage.get().getMessage());
        assertEquals("buyer@team.mobile.de", contactMessage.get().getBuyerMailAddress());
    }

    private void verifyUndefinedMessageCreationDate(Optional<ContactMessage> contactMessage) {

        assertNotNull(contactMessage.get().getMessageCreatedTime());
        assertThat(contactMessage.get().getMessageCreatedTime()).isAfterOrEqualsTo(testStartTime);
        assertThat(contactMessage.get().getMessageCreatedTime()).isBeforeOrEqualsTo(new Date());
    }

    private void verifyReceivedContactMessageWithFields(Optional<ContactMessage> contactMessage) {
        verifyReceivedContactMessageBase(contactMessage);

        assertEquals(new PhoneNumber(49,"123456"), contactMessage.get().getBuyerPhoneNumber());
        assertEquals("GERMANY", contactMessage.get().getSiteId());
        assertEquals("127.0.0.1", contactMessage.get().getIpAddressV4V6());
        assertEquals("DEALER", contactMessage.get().getSellerType());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.DAY_OF_MONTH, 24);
        calendar.set(Calendar.MONTH,1);
        calendar.set(Calendar.YEAR,2015);
        calendar.set(Calendar.HOUR_OF_DAY,14);
        calendar.set(Calendar.MINUTE,8);
        calendar.set(Calendar.SECOND,15);
        calendar.set(Calendar.ZONE_OFFSET,0);
        assertEquals(calendar.getTime(), contactMessage.get().getMessageCreatedTime());
    }

    @SuppressWarnings("serial")
    public static class MockServlet extends HttpServlet {

        private static Optional<ContactMessage> contactMessage;
        private static String jsonResponse = "";

        public static void reset() {
            contactMessage = Optional.empty();
            jsonResponse = "";
        }

        public static void setJsonResponse(String jsonResponse) {
            MockServlet.jsonResponse = jsonResponse;
        }

        public static Optional<ContactMessage> getContactMessage() {
            return contactMessage;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
            InputStreamReader isr = new InputStreamReader(req.getInputStream(), Charsets.UTF_8);
            contactMessage = Optional.of(gson.fromJson(isr, ContactMessage.class));

            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(jsonResponse);

        }
    }
}
