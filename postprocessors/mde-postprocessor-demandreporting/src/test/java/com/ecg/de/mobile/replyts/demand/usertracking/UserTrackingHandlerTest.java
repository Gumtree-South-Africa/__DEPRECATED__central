package com.ecg.de.mobile.replyts.demand.usertracking;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

@RunWith(HierarchicalContextRunner.class)
public class UserTrackingHandlerTest {

    private UserTrackingHandler.Config config;
    private TrackingEventPublisherFactoryMock factory;
    private UserTrackingHandler userTrackingHandler;

    @Before
    public void setup() throws Exception {
        config = createDefaultConfiguration();
        factory = new TrackingEventPublisherFactoryMock();
        userTrackingHandler = new UserTrackingHandler(config, factory);
    }

    private UserTrackingHandler.Config createDefaultConfiguration() {
        UserTrackingHandler.Config config = mock(UserTrackingHandler.Config.class);
        when(config.getApiUrl()).thenReturn("http://api.url");
        when(config.getEventBufferSize()).thenReturn(1);
        when(config.getPublishingIntervalInSeconds()).thenReturn(1);
        when(config.getThreadPoolSize()).thenReturn(1);
        return config;
    }

    @Test
    public void eventPublisherWasCalled() throws Exception {
        CountDownLatchRunnable runnable = factory.getRunnable();
        runnable.createCountDownLatch(1);
        CountDownLatch latch = runnable.countDownLatch;

        latch.await();
    }

    public class withMail {

        private Mail mail;
        private MessageProcessingContext context;
        private MutableConversation conversation;
        private Map<String, String> headers = new HashMap<>();
        private Message message;

        @Before
        public void setup() throws Exception {
            mail = mock(Mail.class);
            message = mock(Message.class);
            when(message.getHeaders()).thenReturn(headers);
            context = new MessageProcessingContext(mail, "messageId", new ProcessingTimeGuard(10));
            when(mail.getAdId()).thenReturn("1");
            when(mail.getFrom()).thenReturn("from@mail.de");
            when(mail.getDeliveredTo()).thenReturn("original-to@mail.de");
            when(mail.getSubject()).thenReturn("subject");
            when(mail.getUniqueHeader(anyString())).thenReturn("Max Mustermann <from@mail.de>");
            conversation = mock(MutableConversation.class);
            when(conversation.getImmutableConversation()).thenReturn(conversation);
            when(conversation.getAdId()).thenReturn("COMA1");
            context.setConversation(conversation);
            when(context.getMessage()).thenReturn(message);
        }

        @Test
        public void createsTrackingEventFromMessageProcessingContext() throws Exception {
            headers.put("X-Cust-BUYER_CUSTOMER_ID", "9");
            headers.put("X-Cust-BUYER_DEVICE_ID", "7");
            headers.put("X-Cust-PUBLISHER", "publisher");
            headers.put("X-Track-Useragent", "Useragent");
            headers.put("X-Track-Ip", "Ip");
            headers.put("X-Track-Referrer", "Referrer");
            headers.put("X-Track-Txid", "Txid");
            headers.put("X-Track-Txseq", "1");
            headers.put("X-Track-Abtests", "abTest.1=A;abTest.2=B");
            when(mail.getPlaintextParts()).thenReturn(Arrays.asList("Text part 1", "Text part 2"));
            EmailContactEvent expected = EmailContactEvent.builder()
                    .message(EmailMessage.builder()
                        .ad(AdRef.of(1L, null))
                        .ip("Ip")
                        .plainText("Text part 1\n-------------------------------------------------\nText part 2")
                        .receiverMailAddress("original-to@mail.de")
                        .senderMailAddress("from@mail.de")
                        .replyToMailAddress("from@mail.de")
                        .subject("subject")
                        .build()
                    )
                    .txId("Txid")
                    .txSeq("1")
                    .vi(Vi.of("7", "9"))
                    .build();

            EmailContactEvent actual = userTrackingHandler.createTrackingEventFromContext(context);

            createdEventEqualsExpected(actual, expected);
        }

        private void createdEventEqualsExpected(EmailContactEvent actual, EmailContactEvent expected) {
            actualCommonEventDataEqualExpected(actual.head, expected.head);
            actualMsgEqualExpected(actual.msg, expected.msg);
            assertEqualsVi(actual.vi, expected.vi);
        }

        private void actualMsgEqualExpected(EmailMessage actual, EmailMessage expected) {
            assertEquals(expected.ad.id, actual.ad.id);
            assertEquals(expected.ad.version, actual.ad.version);
            assertEquals(expected.ip, actual.ip);
            assertEquals(expected.plainText, actual.plainText);
            assertEquals(expected.receiverMailAddress, actual.receiverMailAddress);
            assertEquals(expected.senderMailAddress, actual.senderMailAddress);
            assertEquals(expected.replyToMailAddress, actual.replyToMailAddress);
            assertEquals(expected.subject, actual.subject);
        }

        private void actualCommonEventDataEqualExpected(TrackingEvent.Head actual, TrackingEvent.Head expected) {
            assertEquals(expected.app, actual.app);
            assertEquals(expected.ns, actual.ns);
            assertEquals(expected.txId, actual.txId);
            assertEquals(expected.txSeq, actual.txSeq);
        }

    }

    private void assertEqualsVi(Vi actual, Vi expected) {
        assertEquals(expected.cid, actual.cid);
        assertEquals(expected.sub, actual.sub);
    }

    private class CountDownLatchRunnable implements Runnable {

        private CountDownLatch countDownLatch = null;

        CountDownLatch createCountDownLatch(int countDown) {
            countDownLatch = new CountDownLatch(countDown);
            return countDownLatch;
        }

        @Override
        public void run() {
            if (countDownLatch != null) {
                countDownLatch.countDown();
            }
        }
    }

    private class TrackingEventPublisherFactoryMock extends TrackingEventPublisherFactory {

        private CountDownLatchRunnable runnable;

        public TrackingEventPublisherFactoryMock() {
            super(config, null, null);
        }

        @Override
        public Runnable create(BlockingQueue<TrackingEvent> queue) {
            runnable = new CountDownLatchRunnable();
            return runnable;
        }

        CountDownLatchRunnable getRunnable() {
            return runnable;
        }
    }

}
