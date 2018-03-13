package com.ebay.ecg.bolt.replyts.dedupefilter;

import java.util.*;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ecg.replyts.core.api.search.SearchService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeDupeFilterTest {
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

        DeDupeFilter dedupeFilter = new DeDupeFilter(searchService, conversationRepository, config);

        dedupeFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("MINUTES");
        when(config.getLookupInterval()).thenReturn(5);
        dedupeFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("HOURS");
        when(config.getLookupInterval()).thenReturn(1);
        dedupeFilter.getStartTime();

        when(config.getLookupIntervalTimeUnit()).thenReturn("DAYS");
        when(config.getLookupInterval()).thenReturn(1);
        dedupeFilter.getStartTime();
    }

    @Test
    public void testExceptCategories(){
        exceptCat.add(4);

        when(config.getExceptCategories()).thenReturn(exceptCat);

        DeDupeFilter dedupeFilter = new DeDupeFilter(searchService, conversationRepository, config);

        assertTrue(dedupeFilter.isExceptCategory(Collections.singleton("4")));
    }

    @Test
    public void testEscapeCharacters() {
        String test = "This is a test string with not escaped chars < > _ % $ # @ ± § ; , . \n \t and with escaped chars \\ + - ! ( ) : ^ [ ] \" { } ~ * ? | & / ";
        String escaped = new DeDupeFilter(searchService, conversationRepository, config).escape(test);
        String expected = "This is a test string with not escaped chars < > _ % $ # @ ± § ; , . \n \t and with escaped chars \\\\ \\+ \\- \\! \\( \\) \\: \\^ \\[ \\] \\\" \\{ \\} \\~ \\* \\? \\| \\& \\/ ";
        assertEquals(expected, escaped);
    }

//    @Rule
//    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();
//
//    @Test
//    @Ignore("Will fix the integration test once we have merged the core changes to main")
//    public void violatesQuota() throws Exception {
//        String config = "{"
//            + "\"rules\":{"
//              + "\"minimumShouldMatch\":\"80%\","
//              + "\"lookupInterval\":\"1\","
//              + "\"lookupIntervalTimeUnit\":\"MINUTES\","
//              + "\"score\":\"200\","
//              + "\"matchCount\":3"
//            + "},"
//            + "\"runFor\":{"
//              + "\"exceptCategories\":[],"
//              + "\"categories\":[]"
//            + "}"
//          + "}";
//
//        rule.registerConfig(DeDupeFilterFactory.class, (ObjectNode) JsonObjects.parse(config));
//
//        String from = "foo" + System.currentTimeMillis() + "@bar.com";
//
//        for (int i = 0; i < 3; i++) {
//            MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("Test Message"));
//
//            assertEquals(MessageState.SENT, response.getMessage().getState());
//        }
//
//        rule.flushSearchIndex();
//
//        MailInterceptor.ProcessedMail response = rule.deliver(MailBuilder.aNewMail().adId("123").from(from).to("bar@foo.com").htmlBody("Test Message"));
//
//        assertEquals(1, response.getMessage().getProcessingFeedback().size());
//    }
}