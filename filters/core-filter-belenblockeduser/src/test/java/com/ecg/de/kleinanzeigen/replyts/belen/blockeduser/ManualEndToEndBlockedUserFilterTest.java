package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * User: maldana
 * Date: 19.12.12
 * Time: 10:29
 *
 * @author maldana@ebay.de
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-context.xml")
@TestPropertySource("classpath:test.properties")
public class ManualEndToEndBlockedUserFilterTest {
    @Autowired
    private BlockedUserFilterFactory factory;

    @Test
    public void runBlockedFilter() {
        // 'null' as there is no json-config for this plugin

        Filter plugin = factory.createPlugin("plugin-name", null);

        MessageProcessingContext messageContext = mock(MessageProcessingContext.class, Mockito.RETURNS_DEEP_STUBS);

        when(messageContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(messageContext.getConversation().getUserIdFor(any(ConversationRole.class))).thenReturn("registered@belen-qa.de");

        plugin.filter(messageContext);
    }
}
