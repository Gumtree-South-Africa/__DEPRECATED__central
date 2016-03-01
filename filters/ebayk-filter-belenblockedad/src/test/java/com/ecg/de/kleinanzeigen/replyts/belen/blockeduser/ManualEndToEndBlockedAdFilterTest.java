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
public class ManualEndToEndBlockedAdFilterTest {

    @Ignore
    @Test
    public void runBlockedFilter() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-context.xml");
        BlockedAdFilterFactory bean = context.getBean(BlockedAdFilterFactory.class);
        // 'null' as there is no json-config for this plugin
        Filter plugin = bean.createPlugin("plugin-name", null);
        MessageProcessingContext messageContext = mock(MessageProcessingContext.class, Mockito.RETURNS_DEEP_STUBS);
        when(messageContext.getConversation().getAdId()).thenReturn("13485914");

        System.out.println(plugin.filter(messageContext).get(0).getResultState());
    }

}
