package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.indexer.OutdatedEntityReporter;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class OutdatedEntityMonitorTest {

    private MessageRts message1 = mock(MessageRts.class, Mockito.RETURNS_DEEP_STUBS);

    private MessageRts message2 = mock(MessageRts.class, Mockito.RETURNS_DEEP_STUBS);

    private MessageRts message3 = mock(MessageRts.class, Mockito.RETURNS_DEEP_STUBS);

    private List<MessageRts> msgLst;
    private OutdatedEntityReporter reporter = mock(OutdatedEntityReporter.class);

    private OutdatedEntityMonitor monitor;

    @Before
    public void setUp() {

        monitor = new OutdatedEntityMonitor(reporter);
        when(message1.getConversation().getId()).thenReturn("1");
        when(message2.getConversation().getId()).thenReturn("2");
        when(message3.getConversation().getId()).thenReturn("3");
        when(message1.getState()).thenReturn(MessageRtsState.HELD);
        when(message2.getState()).thenReturn(MessageRtsState.HELD);
        when(message3.getState()).thenReturn(MessageRtsState.HELD);

        msgLst = Lists.newArrayList(message1, message2, message3);
    }

    @Test
    public void findsOutdatedMessageInList() {
        when(message1.getState()).thenReturn(MessageRtsState.BLOCKED);
        when(message2.getState()).thenReturn(MessageRtsState.SENT);


        monitor.scan(msgLst, Arrays.asList(MessageRtsState.HELD, MessageRtsState.BLOCKED));

        verify(reporter).reportOutdated(Lists.newArrayList("2"));

    }

    @Test
    public void doesNotInvokeMonitorWhenDataOkayInList() {
        when(message1.getState()).thenReturn(MessageRtsState.BLOCKED);
        when(message2.getState()).thenReturn(MessageRtsState.SENT);


        monitor.scan(msgLst, Arrays.asList(MessageRtsState.HELD, MessageRtsState.BLOCKED, MessageRtsState.SENT));

        verifyZeroInteractions(reporter);

    }
}
