package com.ecg.replyts.app.filterchain;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.configadmin.ConfigurationAdmin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilterChainProcessingTimeTest {


    @Mock
    private ConfigurationAdmin<ResultInspector> filterConfig;
    @Mock
    private FilterListProcessor filterListProcessor;
    @Mock
    private MessageProcessingContext context;

    private FilterChain chain;

    @Before
    public void setup() {
        chain = new FilterChain(filterConfig, filterListProcessor);
    }

    @Test
    public void terminateMessageWhenProcessingTimeExceeded() {

        when(filterListProcessor.processAllFilters(context)).thenThrow(ProcessingTimeExceededException.class);

        chain.filter(context);

        verify(context).terminateProcessing(eq(MessageState.DISCARDED), eq(chain), any());
    }
}
