package com.ecg.de.mobile.replyts.comafilterservice.filters;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ComaFilterServiceTest {

    @Mock
    private ComaFilterServiceClient comaFilterServiceClient;
    @Mock
    private ContactMessage message;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void doNotFilterMessagesWhenDeactivated() throws Exception {
        Collection<String> result = comaFilterServiceDeactivated(comaFilterServiceClient)
                .getFilterResultsForMessage(message);
        assertThat(result).isEmpty();
    }

    @Test
    public void doNotFilterConversationsWhenDeactivated() throws Exception {
        Collection<String> result = comaFilterServiceDeactivated(comaFilterServiceClient)
                .getFilterResultsForConversation(message);
        assertThat(result).isEmpty();
    }

    @Test
    public void serviceRespondsForMessage() throws Exception {
        List<String> expectedResult = Lists.newArrayList("foo filter", "bar filter");

        when(comaFilterServiceClient.getFilterResultsForMessage(message)).thenReturn(expectedResult);

        Collection<String> actualResult = comaFilterServiceWithActivated().getFilterResultsForMessage(message);
        assertThat(expectedResult).containsAll(actualResult);
    }

    @Test
    public void serviceRespondsForConversation() throws Exception {
        List<String> expectedResult = Lists.newArrayList("filter x", "filter y");

        when(comaFilterServiceClient.getFilterResultsForConversation(message)).thenReturn(expectedResult);

        Collection<String> actualResult = comaFilterServiceWithActivated().getFilterResultsForConversation(message);
        assertThat(expectedResult).containsAll(actualResult);
    }

    private ComaFilterService comaFilterServiceWithActivated() {
        return new ComaFilterService(comaFilterServiceClient, true);
    }

    private ComaFilterService comaFilterServiceDeactivated(ComaFilterServiceClient comaFilterServiceClient) {
        return new ComaFilterService(comaFilterServiceClient, false);
    }
}