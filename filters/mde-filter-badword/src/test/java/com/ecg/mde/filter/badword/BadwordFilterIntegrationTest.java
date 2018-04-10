package com.ecg.mde.filter.badword;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.OpenPortFinder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static org.junit.Assert.assertEquals;

public class BadwordFilterIntegrationTest {

    private Server server;
    private final int port_number = OpenPortFinder.findFreePort();

    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule((((Supplier<Properties>) () -> {
        Properties properties = new Properties();
        properties.put("replyts.mobile.badword.csFilterServiceEndpoint", "http://localhost:"+port_number+"/cs-filter-service");
        return properties;
    }).get()));


    @Before
    public void setup() {
        replyTsIntegrationTestRule.registerConfig(
                BadwordFilterFactory.IDENTIFIER,
                null
        );
    }

    @After
    public void cleanup() throws Exception {
        server.stop();
    }

    private void initServer(Class<? extends HttpServlet> clazz) throws Exception {
        server = new Server(port_number);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(clazz, "/*");
        server.start();
    }

    @Test
    public void messageContainsSwearwordsTest() throws Exception {

        initServer(MockBadwordServlet.class);

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a arschloch! "));

        Message message = processedMail.getMessage();
        List<ProcessingFeedback> processingFeedback = message.getProcessingFeedback();

        assertEquals(2, processingFeedback.size());
    }

    @Test
    public void networkFailureTest() throws Exception {

        initServer(HttpServlet.class);

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a sentence! "));

        Message message = processedMail.getMessage();
        List<ProcessingFeedback> processingFeedback = message.getProcessingFeedback();

        assertEquals(2, processingFeedback.size());
    }


    @Test
    public void messageContainsNOSwearwordsTest() throws Exception {

        initServer(MockNoBadwordServlet.class);

        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule
                .deliver(aNewMail()
                        .adId("1234")
                        .from("foo@bar.com")
                        .to("bar@foo.com")
                        .htmlBody("this is a normal sentence! "));

        Message message = processedMail.getMessage();
        List<ProcessingFeedback> processingFeedback = message.getProcessingFeedback();


        assertEquals(0, processingFeedback.size());
    }


    public static class MockNoBadwordServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"badwords\":[],\"emailPhoneUrlFilterResult\":{\"containsPhone\":false,\"containsURL\":false,\"containsEmail\":false}}");
        }
    }


    public static class MockBadwordServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"badwords\":[{\"id\":\"5743db9b10f8b4b764e20d45\",\"creator\":\"TeamTNS\",\"term\":\"Arschloch\",\"stemmed\":\"arschloch\",\"creationTime\":\"May 24, 2016 6:42:03 AM\",\"type\":\"SWEARWORD\"},{\"id\":\"5743dbc610f8b4b764e23956\",\"creator\":\"TeamTNS\",\"term\":\"arschloch\",\"stemmed\":\"arschloch\",\"creationTime\":\"May 24, 2016 6:42:46 AM\",\"type\":\"SWEARWORD\"},{\"id\":\"5743db9b10f8b4b764e20d49\",\"creator\":\"TeamTNS\",\"term\":\"Arschlocher\",\"stemmed\":\"arschlocher\",\"creationTime\":\"May 24, 2016 6:42:03 AM\",\"type\":\"SWEARWORD\"}],\"emailPhoneUrlFilterResult\":{\"containsPhone\":false,\"containsURL\":false,\"containsEmail\":false}}");
        }
    }
}