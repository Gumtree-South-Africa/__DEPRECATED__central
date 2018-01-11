package com.ecg.replyts.bolt.filter.user;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
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
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class BoltUserFilterTest {
    @InjectMocks
    private BoltUserFilter userFilter;

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

    @Test
    public void shouldPassForValidUserNames() throws Exception{
        mockRequest();
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("buyer-name", "validUserName"));

        List<FilterFeedback> feedback = userFilter.filter(context);

        Assert.assertNotNull(feedback);
        Assert.assertTrue(feedback.isEmpty());
    }

    @Test
    public void shouldBlockBlackListedUserNames() throws Exception{
        mockRequest();

        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("buyer-name", "Admin"));
        List<FilterFeedback> feedback = userFilter.filter(context);

        Assert.assertNotNull(feedback);
        Assert.assertFalse(feedback.isEmpty());
    }

    private void mockRequest() {
        UserSnapshot[] snapShot = {userSnapshot1};
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getBuyerId()).thenReturn("buyer@buyer.com");
        when(conversation.getSellerId()).thenReturn("seller@seller.com");
        when(config.getBlackList()).thenReturn(Sets.newHashSet("abc@abc.com"));
        when(config.getBlackListEmailPattern()).thenReturn(Sets.newHashSet(Pattern.compile("@opayq.com$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        when(config.getBlackListUserNamePattern()).thenReturn(Sets.newHashSet(Pattern.compile("^Admin", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        when(config.getKernelApiUrl()).thenReturn("http://localhost");
        when(template.getForObject(any(URI.class), Matchers.<Class<UserSnapshot[]>>any())).thenReturn(snapShot);
    }
}