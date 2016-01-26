package com.ecg.replyts.app.filterchain;

import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import com.ecg.replyts.core.runtime.configadmin.PluginInstanceReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilterListProcessorTest {

    @Mock
    private FilterListProcessor processor;
    @Mock
    private PluginInstanceReference<Filter> reference1;
    @Mock
    private PluginInstanceReference<Filter> reference2;
    @Mock
    private ConfigurationAdmin<Filter> config;
    @Mock
    private MessageProcessingContext context;
    @Mock
    private ProcessingTimeGuard processingTimeGuard;
    @Mock
    private PluginConfiguration filterConfig;
    @Mock
    private ConfigurationId configId;
    @Mock
    private Metrics metric;
    @Mock
    private Filter filter1;
    @Mock
    private Filter filter2;

    private final List<PluginInstanceReference<Filter>> filterReferences = new ArrayList<>();

    @Before
    public void setUp() {
        processor = new FilterListProcessor(config, metric);

        when(filterConfig.getId()).thenReturn(configId);

        when(reference1.getConfiguration()).thenReturn(filterConfig);
        when(reference2.getConfiguration()).thenReturn(filterConfig);

        when(reference1.getCreatedService()).thenReturn(filter1);
        when(reference2.getCreatedService()).thenReturn(filter2);

        filterReferences.add(reference1);
        filterReferences.add(reference2);
        when(config.getRunningServices()).thenReturn(filterReferences);
        when(context.getProcessingTimeGuard()).thenReturn(processingTimeGuard);
    }

    @Test(expected = ProcessingTimeExceededException.class)
    public void stopProcessingIfTimeExceeded() throws ProcessingTimeExceededException {

        doThrow(ProcessingTimeExceededException.class).when(processingTimeGuard).check();

        processor.processAllFilters(context);
    }
}
