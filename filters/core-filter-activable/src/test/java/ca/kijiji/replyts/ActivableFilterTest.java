package ca.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActivableFilterTest {

    private ActivableFilter objectUnderTest;

    @Mock
    private MessageProcessingContext messageContextMock;

    @Mock
    private Message messageMock;

    @Before
    public void setUp() {
        JsonNode jsonNode = JsonObjects.parse("" +
                "{" +
                "    state: \"ENABLED\", " +
                "    runFor: {" +
                "      categories: [10, 174]," +
                "      exceptCategories: [14]," +
                "      userType: [\"DEALER\"]" +
                "    }" +
                "}");
        objectUnderTest = createFilter(jsonNode);
        when(messageContextMock.getMessage()).thenReturn(messageMock);
    }

    @Test
    public void whenCategoryAndUserTypeAreCovered_thenFilterIsInvoked() {
        when(messageMock.getHeaders()).thenReturn(createHeaders("DEALER", "10, 11"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).hasSize(1);
    }

    @Test
    public void whenCategoryAndUserTypeAreNotCovered_thenFilterIsSkipped() {
        when(messageMock.getHeaders()).thenReturn(createHeaders("DEALER", "112, 135"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenCategoryIsNotCovered_thenFilterIsSkipped() {
        when(messageMock.getHeaders()).thenReturn(createHeaders("FSBO", "112, 135"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenUserTypeIsNotCovered_thenFilterIsSkipped() {
        when(messageMock.getHeaders()).thenReturn(createHeaders("FSBO", "10, 11"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenCategoryIsExcepted_thenFilterIsSkipped() {
        when(messageMock.getHeaders()).thenReturn(createHeaders("DEALER", "10, 14"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    @Test
    public void whenRunForNotConfigured_thenFilterIsInvoked() {
        objectUnderTest = createFilter(JsonObjects.parse("{ state: \"ENABLED\"}"));
        when(messageMock.getHeaders()).thenReturn(createHeaders("DEALER", "10, 14"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).hasSize(1);
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
        objectUnderTest = createFilter(jsonNode);
        when(messageMock.getHeaders()).thenReturn(createHeaders("DEALER", "15, 30"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).hasSize(1);
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
        objectUnderTest = createFilter(jsonNode);
        when(messageMock.getHeaders()).thenReturn(createHeaders("FSBO", "15, 30"));

        List<FilterFeedback> actualFeedback = objectUnderTest.filter(messageContextMock);

        assertThat(actualFeedback).isEmpty();
    }

    private static ActivableFilter createFilter(JsonNode jsonNode) {
        return new ActivableFilter(new Activation(jsonNode)) {
            @Override
            protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
                return Lists.newArrayList(new FilterFeedback("", "", 1, FilterResultState.OK));
            }
        };
    }

    private static Map<String, String> createHeaders(String userType, String categoryPath) {
        return ImmutableMap.of(
                MailHeader.USER_TYPE.getHeaderName(), userType,
                MailHeader.CATEGORY_PATH.getHeaderName(), categoryPath
        );
    }
}
