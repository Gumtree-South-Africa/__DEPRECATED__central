package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
public class ManualEndToEndBlockedUserFilterTest {

    @Ignore
    @Test
    public void runBlockedFilter() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-context.xml");
        BlockedUserFilterFactory bean = context.getBean(BlockedUserFilterFactory.class);
        // 'null' as there is no json-config for this plugin
        Filter plugin = bean.createPlugin("plugin-name", null);
        MessageProcessingContext messageContext = mock(MessageProcessingContext.class, Mockito.RETURNS_DEEP_STUBS);
        when(messageContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(messageContext.getConversation().getUserIdFor(any(ConversationRole.class))).thenReturn("registered@belen-qa.de");

        plugin.filter(messageContext);
    }

}
