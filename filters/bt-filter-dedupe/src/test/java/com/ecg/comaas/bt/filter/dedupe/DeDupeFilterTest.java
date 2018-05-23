package com.ecg.comaas.bt.filter.dedupe;

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
}