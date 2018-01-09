package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WordfilterTest {

    public static final PatternEntry FOOSTR_PATTERN = new PatternEntry(Pattern.compile("FooStr[a-z]*"), 100, Optional.<List>absent());
    //public static final PatternEntry SUBJECT_PATTERN = new PatternEntry(Pattern.compile("subject"), 100, Optional.<List>absent());

    public static final PatternEntry ANY_CHARACTER_PATTERN = new PatternEntry(Pattern.compile("."), 200, Optional.<List>absent());
    private static final PatternEntry NOT_EXISTANT_PATTERN = new PatternEntry(Pattern.compile("googahhhbaaah"), 300, Optional.<List>absent());

    @Mock
    private MessageProcessingContext mpc;

    @Mock
    private Message msg;

    @Mock
    private Conversation conversation;

    @Mock
    private Mail mail;

    @Before
    public void setUp() throws Exception {
        when(msg.getId()).thenReturn("msgid1");
        when(mpc.getMessage()).thenReturn(msg);
        when(msg.getPlainTextBody()).thenReturn("Foobar FooString FooBooh Doh!");
        when(mpc.getConversation()).thenReturn(conversation);
        when(mpc.getMail()).thenReturn(mail);
        when(conversation.getId()).thenReturn("cid");
        // Checkout there is a flag for supporting only partial diff in WordFilter.class
        when(msg.getPlainTextBodyDiff(any(Conversation.class))).thenReturn("Foobar FooString FooBooh Doh!");
        //when(mail.getSubject()).thenReturn("this is the subject");
    }

    @Test
    public void matchesSinglePatternInBody() throws Exception {
        List<FilterFeedback> fb = filter(FOOSTR_PATTERN);

        assertEquals(1, fb.size());
    }

//    @Test
//    public void matchesSinglePatternInSubject() throws Exception {
//        List<FilterFeedback> fb = filter(SUBJECT_PATTERN);
//
//        assertEquals(1, fb.size());
//    }


    @Test
    public void onePatternHitOnlyGeneratesOneFeedback() throws Exception {
        List<FilterFeedback> fb = filter(ANY_CHARACTER_PATTERN);

        assertEquals(1, fb.size());
    }

    @Test
    public void multiplePatternsMatch() throws Exception {
        List<FilterFeedback> fb = filter(ANY_CHARACTER_PATTERN, FOOSTR_PATTERN);

        assertEquals(2, fb.size());
    }

    @Test
    public void emptyListReturnedOnNoPatternMatch() throws Exception {
        List<FilterFeedback> fb = filter(NOT_EXISTANT_PATTERN);

        assertEquals(0, fb.size());
    }

    private List<FilterFeedback> filter(PatternEntry... pattern) {
        return new Wordfilter(new FilterConfig(true, newArrayList(pattern))).filter(mpc);
    }

    @Test
    public void generatesRightProcessingFeedbackOutput() throws Exception {
        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);

        assertEquals(100l, result.getScore().longValue());
        assertEquals("Matched word FooString", result.getDescription());
        assertEquals("FooStr[a-z]*", result.getUiHint());
    }

    @Test
    public void firesWithNormalScoreIfIgnoreDuplicatesInOnOnNoDuplicate() {
        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);
        assertEquals(100l, result.getScore().longValue());
    }

    @Test
    public void filterWithMatchingCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "216"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("Foobar"), 100, Optional.<List>of(Arrays.asList("44","216"))));

        assertThat(fb).hasSize(1);
        assertThat(fb.get(0).getScore()).isEqualTo(100);
    }

    @Test
    public void doNotFilterOnNonMatchingCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "212"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("Foobar"), 100, Optional.<List>of(Arrays.asList("44","216"))));

        assertThat(fb).isEmpty();
    }

    @Test
    public void allwaysFilterOnAbsentPatternCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "212"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("Foobar"), 100, Optional.<List>absent()));

        assertThat(fb).hasSize(1);
        assertThat(fb.get(0).getScore()).isEqualTo(100);
    }

    @Test
    public void firesWithZeroScoreifDupliateIsFoundOnIgnoreDuplicates() {
        Message previousMessage = mock(Message.class);
        when(previousMessage.getId()).thenReturn("previousMsg");
        when(previousMessage.getState()).thenReturn(MessageState.SENT);
        when(previousMessage.getProcessingFeedback()).thenReturn(Arrays.<ProcessingFeedback>asList(new ImmutableProcessingFeedback(WordfilterFactory.class.getName(), "sampleinstance", "FooStr[a-z]*", "desc", 100, FilterResultState.OK, false)));

        when(conversation.getMessages()).thenReturn(Arrays.asList(previousMessage));

        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);
        assertEquals(0l, result.getScore().longValue());
    }
}
