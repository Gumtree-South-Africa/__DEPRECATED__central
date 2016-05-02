package ca.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class ActivableFilterTest {

    @Tested
    ActivableFilter activableFilter;

    @Mocked
    private MessageProcessingContext context;

    @Mocked
    Message message;

    @Before
    public void setup() {

        JsonNode jsonNode = JsonObjects.parse("" +
                "{" +
                "    state: \"ENABLED\", " +
                "    runFor: {" +
                "      categories: [10, 174]," +
                "      exceptCategories: [14]," +
                "      userType: [\"DEALER\"]" +
                "    }" +
                "}");

        activableFilter = new ActivableFilter(new Activation(jsonNode)) {
            @Override
            protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
                return Lists.newArrayList(new FilterFeedback("", "", 1, FilterResultState.OK));
            }
        };

        new NonStrictExpectations() {{
            context.getMessage();
            result = message;
        }};
    }

    @Test
    public void whenCategoryAndUserTypeAreCovered_thenFilterIsInvoked() {
        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "DEALER",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "10, 11"
            );
        }};

        assertThatFilterIsInvoked(activableFilter.filter(context));
    }

    @Test
    public void whenCategoryAndUserTypeAreNotCovered_thenFilterIsSkipped() {
        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "FSBO",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "112, 135"
            );
        }};

        assertThatFilterIsSkipped(activableFilter.filter(context));
    }

    @Test
    public void whenCategoryIsNotCovered_thenFilterIsSkipped() {
        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "DEALER",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "112, 135"
            );
        }};

        assertThatFilterIsSkipped(activableFilter.filter(context));
    }

    @Test
    public void whenUserTypeIsNotCovered_thenFilterIsSkipped() {
        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "FSBO",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "10, 11"
            );
        }};

        assertThatFilterIsSkipped(activableFilter.filter(context));
    }

    @Test
    public void whenCategoryIsExcepted_thenFilterIsSkipped() {
        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "DEALER",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "10, 14"
            );
        }};

        assertThatFilterIsSkipped(activableFilter.filter(context));
    }

    @Test
    public void whenRunForNotConfigured_thenFilterIsInvoked() {
        JsonNode jsonNode = JsonObjects.parse("{ state: \"ENABLED\"}");

        ActivableFilter activableFilter = new ActivableFilter(new Activation(jsonNode)) {
            @Override
            protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
                return Lists.newArrayList(new FilterFeedback("", "", 1, FilterResultState.OK));
            }
        };

        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "DEALER",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "10, 14"
            );
        }};

        assertThatFilterIsInvoked(activableFilter.filter(context));
    }

    @Test
    public void givenRunForConfiguredForUserTypeOnly_whenUserTypeMatches_thenFilterIsInvokedForAnyCategory() {
        JsonNode jsonNode = JsonObjects.parse("" +
                "{" +
                "    state: \"ENABLED\", " +
                "    runFor: {" +
                "      userType: [\"DEALER\"]" +
                "    }" +
                "}");

        ActivableFilter activableFilter = new ActivableFilter(new Activation(jsonNode)) {
            @Override
            protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
                return Lists.newArrayList(new FilterFeedback("", "", 1, FilterResultState.OK));
            }
        };

        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "DEALER",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "15, 30"
            );
        }};

        assertThatFilterIsInvoked(activableFilter.filter(context));
    }

    @Test
    public void givenRunForConfiguredForUserTypeOnly_whenUserTypeDoesNotMatch_thenFilterIsSkippedForAnyCategory() {
        JsonNode jsonNode = JsonObjects.parse("" +
                "{" +
                "    state: \"ENABLED\", " +
                "    runFor: {" +
                "      userType: [\"DEALER\"]" +
                "    }" +
                "}");

        ActivableFilter activableFilter = new ActivableFilter(new Activation(jsonNode)) {
            @Override
            protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
                return Lists.newArrayList(new FilterFeedback("", "", 1, FilterResultState.OK));
            }
        };

        new NonStrictExpectations() {{
            message.getHeaders();
            result = ImmutableMap.of(
                    MailHeader.USER_TYPE.getHeaderName(), "FSBO",
                    MailHeader.CATEGORY_PATH.getHeaderName(), "15, 30"
            );
        }};

        assertThatFilterIsSkipped(activableFilter.filter(context));
    }

    private void assertThatFilterIsInvoked(List<FilterFeedback> feedbacks) {
        assertThat(feedbacks.size(), CoreMatchers.equalTo(1));
    }

    private void assertThatFilterIsSkipped(List<FilterFeedback> feedbacks) {
        assertThat(feedbacks.size(), CoreMatchers.equalTo(0));
    }
}
