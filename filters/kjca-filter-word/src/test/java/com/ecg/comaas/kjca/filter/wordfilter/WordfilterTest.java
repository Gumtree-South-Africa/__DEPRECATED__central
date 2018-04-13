package com.ecg.comaas.kjca.filter.wordfilter;

import com.ecg.comaas.core.filter.activable.Activation;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WordfilterTest {

    private static final PatternEntry FOOSTR_PATTERN = new PatternEntry(Pattern.compile("FooStr[a-z]*"), 100, Collections.emptyList());
    private static final PatternEntry SUBJECT_PATTERN = new PatternEntry(Pattern.compile("subject"), 100, Collections.emptyList());
    private static final PatternEntry ANY_CHARACTER_PATTERN = new PatternEntry(Pattern.compile("."), 200, Collections.emptyList());
    private static final PatternEntry NOT_EXISTANT_PATTERN = new PatternEntry(Pattern.compile("googahhhbaaah"), 300, Collections.emptyList());

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Message msg;

    @Mock
    private Conversation conversation;

    @Mock
    private Mail mail;

    @Mock
    private ProcessingTimeGuard timeGuard;

    @Before
    public void setUp() throws Exception {
        when(msg.getId()).thenReturn("msgid1");
        when(context.getMessage()).thenReturn(msg);
        when(msg.getPlainTextBody()).thenReturn("Foobar FooString FooBooh Doh!");
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(conversation.getId()).thenReturn("cid");
        // Checkout there is a flag for supporting only partial diff in WordFilter.class
        when(msg.getPlainTextBodyDiff(any(Conversation.class))).thenReturn("Foobar FooString FooBooh Doh!");
        when(mail.getSubject()).thenReturn("this is the subject");
        when(context.getProcessingTimeGuard()).thenReturn(timeGuard);
    }

    @Test
    public void matchesSinglePatternInBody() throws Exception {
        List<FilterFeedback> fb = filter(FOOSTR_PATTERN);

        assertThat(fb).hasSize(1);
    }

    @Test
    public void matchesSinglePatternInSubject() throws Exception {
        List<FilterFeedback> fb = filter(SUBJECT_PATTERN);

        assertThat(fb).hasSize(1);
    }


    @Test
    public void onePatternHitOnlyGeneratesOneFeedback() throws Exception {
        List<FilterFeedback> fb = filter(ANY_CHARACTER_PATTERN);

        assertThat(fb).hasSize(1);
    }

    @Test
    public void multiplePatternsMatch() throws Exception {
        List<FilterFeedback> fb = filter(ANY_CHARACTER_PATTERN, FOOSTR_PATTERN);

        assertThat(fb).hasSize(2);
    }

    @Test
    public void emptyListReturnedOnNoPatternMatch() throws Exception {
        List<FilterFeedback> fb = filter(NOT_EXISTANT_PATTERN);

        assertThat(fb).isEmpty();
    }

    private List<FilterFeedback> filter(PatternEntry... pattern) {
        return new Wordfilter(new FilterConfig(true, true, newArrayList(pattern)), new Activation(new ObjectNode(JsonNodeFactory.instance)), 1000L).filter(context);
    }

    @Test
    public void generatesRightProcessingFeedbackOutput() throws Exception {
        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);

        assertThat(result.getScore().longValue()).isEqualTo(100L);
        assertThat(result.getDescription()).isEqualTo("Matched word FooString");
        assertThat(result.getUiHint()).isEqualTo("FooStr[a-z]*");
    }

    @Test
    public void firesWithNormalScoreIfIgnoreDuplicatesInOnOnNoDuplicate() {
        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);

        assertThat(result.getScore().longValue()).isEqualTo(100L);
    }

    @Test
    public void filterWithMatchingCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "216"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("subject"), 100, asList("44", "216")));

        assertThat(fb).hasSize(1);
        assertThat(fb.get(0).getScore()).isEqualTo(100);
    }

    @Test
    public void doNotFilterOnNonMatchingCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "212"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("subject"), 100, asList("44", "216")));

        assertThat(fb).isEmpty();
    }

    @Test
    public void allwaysFilterOnAbsentPatternCategory() {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of(Wordfilter.CATEGORY_ID, "212"));

        List<FilterFeedback> fb = filter(new PatternEntry(Pattern.compile("subject"), 100, Collections.emptyList()));

        assertThat(fb).hasSize(1);
        assertThat(fb.get(0).getScore()).isEqualTo(100);
    }

    public void callTimeGuard() {

        filter(new PatternEntry(Pattern.compile("subject"), 100, Collections.emptyList()));

        verify(timeGuard).check();
    }

    @Test
    public void firesWithZeroScoreifDupliateIsFoundOnIgnoreDuplicates() {
        Message previousMessage = mock(Message.class);
        when(previousMessage.getId()).thenReturn("previousMsg");
        when(previousMessage.getState()).thenReturn(MessageState.SENT);
        when(previousMessage.getProcessingFeedback()).thenReturn(Collections.singletonList(new ImmutableProcessingFeedback(WordfilterFactory.IDENTIFIER, "sampleinstance", "FooStr[a-z]*", "desc", 100, FilterResultState.OK, false)));

        when(conversation.getMessages()).thenReturn(Collections.singletonList(previousMessage));

        FilterFeedback result = filter(FOOSTR_PATTERN).get(0);
        assertThat(result.getScore().longValue()).isEqualTo(0L);
    }
}
