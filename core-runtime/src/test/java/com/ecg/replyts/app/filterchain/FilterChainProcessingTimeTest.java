package com.ecg.replyts.app.filterchain;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FilterChainProcessingTimeTest.TestContext.class)
@Import(FilterChain.class)
public class FilterChainProcessingTimeTest {
    @MockBean
    private FilterListProcessor filterListProcessor;

    @MockBean
    private MessageProcessingContext context;

    @MockBean
    private HeldMailRepository heldMailRepository;

    @Autowired
    private FilterChain chain;

    @Test
    public void terminateMessageWhenProcessingTimeExceeded() {
        when(filterListProcessor.processAllFilters(context)).thenThrow(ProcessingTimeExceededException.class);

        chain.filter(context);

        verify(context).terminateProcessing(eq(MessageState.DISCARDED), eq(chain), any());
    }

    @Configuration
    static class TestContext {
        @MockBean
        private ClusterRefreshPublisher clusterRefreshPublisher;

        @Bean
        public ConfigurationAdmin<ResultInspector> resultInspectorConfigurationAdmin() {
            return mock(ConfigurationAdmin.class);
        }
    }
}
