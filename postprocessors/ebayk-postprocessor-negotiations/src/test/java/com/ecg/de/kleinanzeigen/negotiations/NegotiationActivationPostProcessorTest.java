package com.ecg.de.kleinanzeigen.negotiations;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ibm.icu.impl.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class NegotiationActivationPostProcessorTest {

    @InjectMocks
    private NegotiationActivationPostProcessor activationPostProcessor;
    @Mock
    private Caller caller;
    @Mock
    private MessageProcessingContext context;

    @Test(expected = RuntimeException.class)
    public void testRethrowRuntimeException() {
        doThrow(new RuntimeException())
                .when(caller).execute(any(Caller.NegotationState.class), any(Conversation.class), any(Message.class));

        activationPostProcessor.postProcess(context);

        Assert.fail("failed");
    }
}