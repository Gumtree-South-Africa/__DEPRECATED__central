package com.ecg.replyts.app;

import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.preprocessorchain.PreProcessorManager;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(ProcessingFlow.class)
public class ProcessingFlowTest {
    @MockBean
    private PreProcessorManager preProcessor;

    @MockBean
    private FilterChain filterChain;

    @MockBean
    private PostProcessorChain postProcessor;

    @MockBean
    private MailDeliveryService mailDeliveryService;

    @MockBean
    private MessageProcessingContext context;

    @MockBean
    private MutableMail mail;

    @Autowired
    private ProcessingFlow flow;

    @Before
    public void setUp() throws Exception {
        when(context.getOutgoingMail()).thenReturn(mail);
    }

    @Test
    public void correctMailRunsThroughFromPreprocessor() throws Exception {
        flow.inputForPreProcessor(context);
        verify(preProcessor).preProcess(context);
        verify(filterChain).filter(context);
        verify(postProcessor).postProcess(context);
        verify(mailDeliveryService).deliverMail(mail);
    }

    @Test
    public void correctMailRunsThroughFromFilterchain() throws Exception {
        flow.inputForFilterChain(context);
        verify(preProcessor, times(0)).preProcess(context);
        verify(filterChain).filter(context);
        verify(postProcessor).postProcess(context);
        verify(mailDeliveryService).deliverMail(mail);
    }

    @Test
    public void correctMailRunsThroughFromPostprocessor() throws Exception {
        flow.inputForPostProcessor(context);
        verify(preProcessor, times(0)).preProcess(context);
        verify(filterChain, times(0)).filter(context);
        verify(postProcessor).postProcess(context);
        verify(mailDeliveryService).deliverMail(mail);
    }

    @Test
    public void correctMailRunsThroughFromSending() throws Exception {
        flow.inputForSending(context);
        verify(preProcessor, times(0)).preProcess(context);
        verify(filterChain, times(0)).filter(context);
        verify(postProcessor, times(0)).postProcess(context);
        verify(mailDeliveryService).deliverMail(mail);
    }

    @Test(expected = IllegalStateException.class)
    public void verifyMailMayNotBeTerminatedByPostProcessor() throws Exception {
        when(context.isTerminated()).thenReturn(true);
        flow.inputForPostProcessor(context);
    }

    @Test
    public void verifyMailMayNotBeForwardedToFilterChainWhenTerminated() {
        when(context.isTerminated()).thenReturn(true);
        flow.inputForPreProcessor(context);
        verify(filterChain, times(0)).filter(context);
    }

    @Test
    public void verifyMailMayNotBeForwardedToPostProcessorWhenTerminated() {
        when(context.isTerminated()).thenReturn(true);
        flow.inputForFilterChain(context);
        verify(postProcessor, times(0)).postProcess(context);
    }

    @Test
    public void verifyMailIsNotSentWhenMailChannelIsDisabled() throws Exception {
        when(context.isSkipDeliveryChannel(eq(MessageProcessingContext.DELIVERY_CHANNEL_MAIL))).thenReturn(true);
        flow.inputForSending(context);
        verify(mailDeliveryService,never()).deliverMail(any(Mail.class));
    }

    @Test
    public void mailRejectedByOutgoingServerGetsFixedAndResent() throws Exception {
        MailDeliveryException mailDeliveryException = new MailDeliveryException("javax.mail.internet.ParseException");
        doThrow(mailDeliveryException)
                .doNothing()
                .when(mailDeliveryService).deliverMail(mail);

        flow.inputForSending(context);

        verify(mail, times(1)).applyOutgoingMailFixes(ImmutableList.of(), mailDeliveryException);
    }

    @Test(expected = RuntimeException.class)
    public void mailRejectedByOutgoingServerCannotBeFixedNotSent() throws Exception {
        MailDeliveryException mailDeliveryException = new MailDeliveryException("javax.mail.internet.ParseException");
        doThrow(mailDeliveryException)
                .when(mailDeliveryService).deliverMail(mail);

        flow.inputForSending(context);

        verify(mail, times(1)).applyOutgoingMailFixes(ImmutableList.of(), mailDeliveryException);
    }
}
