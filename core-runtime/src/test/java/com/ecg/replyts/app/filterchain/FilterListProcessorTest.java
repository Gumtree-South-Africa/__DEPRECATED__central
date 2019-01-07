package com.ecg.replyts.app.filterchain;

import com.ecg.replyts.core.api.configadmin.ConfigurationLabel;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.configadmin.ClusterRefreshPublisher;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import com.ecg.replyts.core.runtime.configadmin.PluginInstanceReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FilterListProcessorTest.TestContext.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(FilterListProcessor.class)
public class FilterListProcessorTest {
    @MockBean
    private MessageProcessingContext context;

    @MockBean
    private ProcessingTimeGuard processingTimeGuard;

    @MockBean
    private PluginConfiguration filterConfig;

    private ConfigurationLabel configId = new ConfigurationLabel("pluginFactory", "instance");

    @MockBean
    private FilterListMetrics metrics;

    @Autowired
    private PluginInstanceReference<Filter> reference1;

    @Autowired
    private PluginInstanceReference<Filter> reference2;

    @Autowired
    private ConfigurationAdmin<Filter> config;

    @Autowired
    private Filter filter1;

    @Autowired
    private Filter filter2;

    @Autowired
    private FilterListProcessor processor;

    private final List<PluginInstanceReference<Filter>> filterReferences = new ArrayList<>();

    @Before
    public void setUp() {
        when(filterConfig.getLabel()).thenReturn(configId);

        when(reference1.getConfiguration()).thenReturn(filterConfig);
        when(reference2.getConfiguration()).thenReturn(filterConfig);

        when(reference1.getCreatedService()).thenReturn(filter1);
        when(reference2.getCreatedService()).thenReturn(filter2);

        filterReferences.add(reference1);
        filterReferences.add(reference2);
        when(config.getRunningServices()).thenReturn(filterReferences);
        when(context.getProcessingTimeGuard()).thenReturn(processingTimeGuard);
        when(context.getConversation()).thenReturn(mock(Conversation.class));
    }

    @Test
    public void processAllFilters()
    {
        when(filter1.filter(context)).thenReturn(Arrays.asList(new FilterFeedback("", "", 1, FilterResultState.OK)));

        List<ProcessingFeedback> result = processor.processAllFilters(context);

        assertThat(result).hasSize(1);
    }

    @Test(expected = ProcessingTimeExceededException.class)
    public void stopProcessingIfTimeExceeded() throws ProcessingTimeExceededException {
        doThrow(ProcessingTimeExceededException.class).when(processingTimeGuard).check();

        processor.processAllFilters(context);

        shouldHaveThrown(ProcessingTimeExceededException.class);
    }

    @Configuration
    static class TestContext {
        @MockBean
        private ClusterRefreshPublisher clusterRefreshPublisher;

        @Bean
        public Filter filter1() {
            return mock(Filter.class);
        }

        @Bean
        public Filter filter2() {
            return mock(Filter.class);
        }

        @Bean
        public PluginInstanceReference<Filter> reference1() {
            return mock(PluginInstanceReference.class);
        }

        @Bean
        public PluginInstanceReference<Filter> reference2() {
            return mock(PluginInstanceReference.class);
        }

        @Bean
        public ConfigurationAdmin<Filter> filterConfigurationAdmin() {
            return mock(ConfigurationAdmin.class);
        }
    }
}
