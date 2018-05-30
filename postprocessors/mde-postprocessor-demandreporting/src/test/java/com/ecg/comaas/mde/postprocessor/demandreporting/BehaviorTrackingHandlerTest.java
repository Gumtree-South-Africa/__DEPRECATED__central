package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.domain.CommonEventData;
import com.ecg.comaas.mde.postprocessor.demandreporting.domain.EmailContactEvent;
import com.ecg.comaas.mde.postprocessor.demandreporting.domain.Event;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(HierarchicalContextRunner.class)
public class BehaviorTrackingHandlerTest {

    private BehaviorTrackingHandler.Config config;
    private EventPublisherFactoryMock factory;
    private BehaviorTrackingHandler behaviorTrackingHandler;

    @Before
    public void setup() throws Exception {
        config = createDefaultConfiguration();
        factory = new EventPublisherFactoryMock();
        behaviorTrackingHandler = new BehaviorTrackingHandler(config, factory);
    }

    private BehaviorTrackingHandler.Config createDefaultConfiguration() {
        BehaviorTrackingHandler.Config config = mock(BehaviorTrackingHandler.Config.class);
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
            context = new MessageProcessingContext(mail, "messageId", new ProcessingTimeGuard(0L));
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
            headers.put("X-Track-Abtests", "abTest.1=A;abTest.2=B");
            when(mail.getPlaintextParts()).thenReturn(Arrays.asList("Text part 1", "Text part 2"));
            EmailContactEvent expected = EmailContactEvent.builder()
                    .adId(1L)
                    .content("Text part 1\n-------------------------------------------------\nText part 2")
                    .receiverMailAddress("original-to@mail.de")
                    .senderMailAddress("from@mail.de")
                    .replyToMailAddress("from@mail.de")
                    .source("mobile")
                    .subject("subject")
                    .buildWithCommonEventData(CommonEventData.builder()
                            .deviceId("7")
                            .userId("9")
                            .publisher("publisher")
                            .ip("Ip")
                            .referrer("Referrer")
                            .txId("Txid")
                            .userAgent("Useragent")
                            .activeVariantForAbTest("A", "abTest.1")
                            .activeVariantForAbTest("B", "abTest.2")
                            .build());

            EmailContactEvent actual = behaviorTrackingHandler.createTrackingEventFromContext(context);

            createdEventEqualsExpected(actual, expected);
        }

        private void createdEventEqualsExpected(EmailContactEvent actual, EmailContactEvent expected) {
            actualCommonEventDataEqualExpected(actual.getCommonEventData(), expected.getCommonEventData());
            assertEquals(expected.getAdId(), actual.getAdId());
            assertEquals(expected.getContent(), actual.getContent());
            assertEquals(expected.getReceiverMailAddress(), actual.getReceiverMailAddress());
            assertEquals(expected.getReplyToMailAddress(), actual.getReplyToMailAddress());
            assertEquals(expected.getSenderMailAddress(), actual.getSenderMailAddress());
            assertEquals(expected.getSubject(), actual.getSubject());
        }

        private void actualCommonEventDataEqualExpected(CommonEventData actual, CommonEventData expected) {
            assertEquals(expected.getUserId(), actual.getUserId());
            assertEquals(expected.getDeviceId(), actual.getDeviceId());
            assertEquals(expected.getPublisher(), actual.getPublisher());
            assertEquals(expected.getUserAgent(), actual.getUserAgent());
            assertEquals(expected.getIp(), actual.getIp());
            assertEquals(expected.getTxId(), actual.getTxId());
            assertEquals(expected.getAllAbTestsWithActiveVariant(), actual.getAllAbTestsWithActiveVariant());
            assertEquals(expected.getReferrer(), actual.getReferrer());
        }

    }

    private class CountDownLatchRunnable implements Runnable {

        private CountDownLatch countDownLatch = null;

        public CountDownLatch createCountDownLatch(int countDown) {
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

    private class EventPublisherFactoryMock implements EventPublisherFactory {


        private CountDownLatchRunnable runnable;

        @Override
        public Runnable create(BlockingQueue<Event> queue) {
            runnable = new CountDownLatchRunnable();
            return runnable;
        }

        public CountDownLatchRunnable getRunnable() {
            return runnable;
        }
    }

}
