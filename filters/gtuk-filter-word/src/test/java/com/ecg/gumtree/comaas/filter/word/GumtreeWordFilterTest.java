package com.ecg.gumtree.comaas.filter.word;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.Rule;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.WordFilterConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.gumtree.MockFactory.mockConversation;
import static com.ecg.gumtree.MockFactory.mockMessage;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GumtreeWordFilterTest {
    private static final String messageId = "123";

    private GumtreeWordFilter userFilter;

    @Before
    public void setup() {
        WordFilterConfig config = new WordFilterConfig.Builder(State.ENABLED, 0, Result.DROP)
                .withVersion("1.0")
                .withExemptedCategories(ImmutableList.of(1234L, 4321L))
                .withRules(createRules())
                .build();

        userFilter = new GumtreeWordFilter(new Filter("123", "123", null), config);
    }

    private ImmutableList<Rule> createRules() {
        return ImmutableList.of(
                new Rule.Builder("nigeria").withExceptions(ImmutableList.of()).withWordBoundaries(true).build(),
                new Rule.Builder("western union").withExceptions(ImmutableList.of()).withWordBoundaries(true).build(),
                new Rule.Builder("escrow").withExceptions(ImmutableList.of()).withWordBoundaries(true).build(),
                new Rule.Builder("sword").withExceptions(ImmutableList.of()).withWordBoundaries(true).build(),
                new Rule.Builder("gun").withExceptions(ImmutableList.of("top gun", "gun dog")).withWordBoundaries(true).build(),
                new Rule.Builder("Join Now !").withExceptions(ImmutableList.of()).withWordBoundaries(false).build(),
                new Rule.Builder("\\W+child\\W+porn").withExceptions(ImmutableList.of()).withWordBoundaries(false).build(),
                new Rule.Builder("|").withExceptions(ImmutableList.of()).withWordBoundaries(false).build(),
                new Rule.Builder("&").withExceptions(ImmutableList.of()).withWordBoundaries(false).build(),
                new Rule.Builder("Shit").withExceptions(ImmutableList.of("holy shit", "shit car")).withWordBoundaries(true).build(),
                new Rule.Builder("fuck").withExceptions(ImmutableList.of("holy Fuck", "Fuck this")).withWordBoundaries(true).build(),
                new Rule.Builder("\\>").withExceptions(ImmutableList.of()).withWordBoundaries(false).build(),
                new Rule.Builder("<").withExceptions(ImmutableList.of()).withWordBoundaries(false).build());
    }

    @Test
    public void excludedCategoriesReturnsAnEmptyList() {
        Mail mail = mock(Mail.class);
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, messageId, new ProcessingTimeGuard(1L));

        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", ImmutableSet.of(1234));

        messageProcessingContext.setConversation(conversation);

        List<FilterFeedback> feedbacks = userFilter.filter(messageProcessingContext);
        assertEquals(0, feedbacks.size());
    }

    @Test
    public void matchOnGreaterThanWithSlashes() {
        List<FilterFeedback> reasons = filter("This is just a \\> day.");
        assertThat(reasons.size(), equalTo(1));
    }

    @Test
    public void matchOnLessThanWithoutSlashes() {
        List<FilterFeedback> reasons = filter("This is just a < day.");
        assertThat(reasons.size(), equalTo(1));
    }

    @Test
    public void exclusionsAreNotBeingCaughtKeyWordIsCapitalExclusionLowercaseEmailCap() {
        List<FilterFeedback> reasons = filter("This is just a Holy Shit.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void exclusionsAreNotBeingCaughtKeyWordIsCapitalExclusionLowercaseEmailLower() {
        List<FilterFeedback> reasons = filter("This is just a shit car.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void exclusionsAreNotBeingCaughtKeyWordLowercaseExclusionCapital() {
        List<FilterFeedback> reasons = filter("This is just a Holy Fuck.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void exclusionsAreNotBeingCaughtKeyWordLowercaseExclusionCapitalEmailLower() {
        List<FilterFeedback> reasons = filter("This is just a fuck this.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void doesNotFallOverWithBarInFilterList() {
        List<FilterFeedback> reasons = filter("I would like to purchase this product off you.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void doesFilterWithBarCharacter() {
        List<FilterFeedback> reasons = filter("I would like to purchase this | off you.");
        assertThat(reasons.size(), equalTo(1));
    }

    @Test
    public void doesFilterWithAmpCharacter() {
        List<FilterFeedback> reasons = filter("I would like to purchase this & off you.");
        assertThat(reasons.size(), equalTo(1));
    }

    @Test
    public void verifyWhenNoBannedWords() {
        List<FilterFeedback> reasons = filter("I would like to purchase this product off you.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void verifyWhenBannedWord() {
        List<FilterFeedback> reasons = filter("I would like to purchase this product off you via escrow.");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("escrow"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: escrow"));
    }

    @Test
    public void verifyWhenBannedPhrase() {
        List<FilterFeedback> reasons = filter("I would like to purchase this product off you via western union.");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("western union"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: western union"));
    }

    @Test
    public void verifyWhenPartBannedWord() {
        List<FilterFeedback> reasons = filter("Remember not to share your password.");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void verifyWhenNotMatchedWithExclusionsNegativeLookBehind() {
        List<FilterFeedback> reasons = filter("I really love top gun!");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void verifyWhenMatchedWithExclusionsNegativeLookAhead() {
        List<FilterFeedback> reasons = filter("I really love gun top!");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("gun"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: gun"));
    }

    @Test
    public void verifyWhenNotMatchedWithExclusionsNegativeLookAhead() {
        List<FilterFeedback> reasons = filter("I really love my gun dog!");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void verifyWhenMatchedWithExclusionsNegativeLookBehind() {
        List<FilterFeedback> reasons = filter("I really love my dog gun!");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("gun"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: gun"));
    }

    @Test
    public void verifyWhenExclusionsNotMatched() {
        List<FilterFeedback> reasons = filter("You want to buy this semi-automatic machine gun?");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("gun"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: gun"));
    }

    @Test
    public void verifyWhenSingleSpecialCharacter() {
        List<FilterFeedback> reasons = filter("Click here to Join Now !");
        assertThat(reasons.size(), equalTo(1));
        assertThat(reasons.get(0).getResultState(), equalTo(FilterResultState.DROPPED));
        assertThat(reasons.get(0).getUiHint(), equalTo("Join Now !"));
        assertThat(shortDescription(reasons.get(0).getDescription()), equalTo("Matched: Join Now !"));
    }

    @Test
    public void verifyWhenRegexMatched() {
        List<FilterFeedback> reasons = filter("This is a test message for child porn");
        assertThat(reasons.size(), equalTo(0));
    }

    @Test
    public void verifyWordBoundary() {
        List<FilterFeedback> reasons = filter("Live Testing by the QA Team, please ignore. Hey this looks good. I am from Nigeria.");
        assertThat(reasons.size(), equalTo(1));
    }

    private List<FilterFeedback> filter(String mailContent) {
        Mail mail = mock(Mail.class);

        TypedContent<String> content = new TypedContent<String>(MediaType.ANY_TYPE, mailContent) {
            @Override
            public boolean isMutable() {
                return false;
            }

            @Override
            public void overrideContent(String s) throws IllegalStateException {
            }
        };

        when(mail.getTextParts(false)).thenReturn(Arrays.asList(content));

        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("goodguy@hotmail.com", "badguy@hotmail.com", message);

        MessageProcessingContext messageProcessingContext = new MessageProcessingContext(mail, messageId, new ProcessingTimeGuard(1L));

        messageProcessingContext.getFilterContext().put("categoryBreadCrumb", ImmutableSet.of(987));
        messageProcessingContext.setConversation(conversation);

        return userFilter.filter(messageProcessingContext);
    }

    private String shortDescription(String json) {
        Map<String, String> fields = new HashMap();

        return (String) new Gson().fromJson(json, fields.getClass()).get("description");
    }

}