package com.ecg.comaas.bt.filter.identicalcap;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdenticalCapFilterIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Mock
    private SearchService searchService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private FilterConfig config;

    private List<Integer> exceptCat = new ArrayList<>();

    @Test
    public void testEndDate(){
        when(config.getLookupIntervalTimeUnit()).thenReturn("SECONDS");
        when(config.getLookupInterval()).thenReturn(50);

        IdenticalCapFilter idCapFilter = new IdenticalCapFilter(searchService, conversationRepository, config);

        idCapFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("MINUTES");
        when(config.getLookupInterval()).thenReturn(5);
        idCapFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("HOURS");
        when(config.getLookupInterval()).thenReturn(1);
        idCapFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("DAYS");
        when(config.getLookupInterval()).thenReturn(1);
        idCapFilter.getStartTime();
    }

    @Test
    public void testExceptCategories(){
        exceptCat.add(4);

        when(config.getExceptCategories()).thenReturn(exceptCat);

        IdenticalCapFilter idCapFilter = new IdenticalCapFilter(searchService, conversationRepository, config);

        assertTrue(idCapFilter.isExceptCategory(Collections.singleton("4")));
    }

    @Test
    @Ignore
    public void violatesQuota() throws Exception {
        String config = "{"
            + "\"rules\":{"
              + "\"charScanLength\":\"10\","
              + "\"lookupInterval\":\"1\","
              + "\"lookupIntervalTimeUnit\":\"MINUTES\","
              + "\"score\":\"200\","
              + "\"matchCount\":3"
            + "},"
            + "\"runFor\":{"
              + "\"exceptCategories\":[],"
              + "\"categories\":[]"
            + "}"
          + "}";

        rule.registerConfig(IdenticalCapFilterFactory.IDENTIFIER, (ObjectNode) JsonObjects.parse(config));

        String from = "foo" + System.currentTimeMillis() + "@bar.com";

        for (int i = 0; i < 3; i++) {
            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("Test Message"));
            rule.waitUntilIndexedInEs(response);
            assertEquals(MessageState.SENT, response.getMessage().getState());
        }

        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("Test Message"));

        assertEquals(1, response.getMessage().getProcessingFeedback().size());
    }
}