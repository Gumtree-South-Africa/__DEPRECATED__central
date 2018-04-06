package com.ecg.comaas.core.filter.belenblockedad;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
@ContextConfiguration(classes = BlockedAdFilterConfiguration.class)
@TestPropertySource("classpath:test.properties")
public class ManualEndToEndBlockedAdFilterTest {
    @Autowired
    private BlockedAdFilterFactory factory;

    @Test
    public void runBlockedFilter() {
        // 'null' as there is no json-config for this plugin

        Filter plugin = factory.createPlugin("plugin-name", null);

        MessageProcessingContext messageContext = mock(MessageProcessingContext.class, Mockito.RETURNS_DEEP_STUBS);

        when(messageContext.getConversation().getAdId()).thenReturn("13485914");

        System.out.println(plugin.filter(messageContext).get(0).getResultState());
    }
}
