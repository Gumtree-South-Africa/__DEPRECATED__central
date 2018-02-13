package com.ecg.replyts.bolt.filter.user;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BoltUserFilterTest {
    @Mock
    private BoltUserFilterConfig config;

    @Mock
    private RestTemplate template;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private Conversation conversation;

    @Mock
    private UserSnapshot userSnapshot1;

    @InjectMocks
    private BoltUserFilter userFilter;

    @Test
    public void shouldPassForValidUserNames() throws Exception {
        mockRequest();

        when(conversation.getCustomValues()).thenReturn(Collections.singletonMap("buyer-name", "validUserName"));

        List<FilterFeedback> feedback = userFilter.filter(context);

        Assert.assertNotNull(feedback);
        Assert.assertTrue(feedback.isEmpty());
    }

    @Test
    public void shouldBlockBlackListedUserNames() throws Exception {
        mockRequest();

        when(conversation.getCustomValues()).thenReturn(Collections.singletonMap("buyer-name", "Admin"));

        List<FilterFeedback> feedback = userFilter.filter(context);

        Assert.assertNotNull(feedback);
        Assert.assertFalse(feedback.isEmpty());
    }

    private void mockRequest() {
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(config.getBlackList()).thenReturn(Collections.singleton("abc@abc.com"));
        when(config.getBlackListEmailPattern()).thenReturn(Collections.singleton(Pattern.compile("@opayq.com$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        when(config.getBlackListUserNamePattern()).thenReturn(Collections.singleton(Pattern.compile("^Admin", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        when(config.getKernelApiUrl()).thenReturn("http://localhost");
        when(template.getForObject(any(URI.class), Matchers.<Class<UserSnapshot[]>>any())).thenReturn(new UserSnapshot[] { userSnapshot1 });
    }
}