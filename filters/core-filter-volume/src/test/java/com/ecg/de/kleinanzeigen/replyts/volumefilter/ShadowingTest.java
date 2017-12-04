package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.de.kleinanzeigen.replyts.volumefilter.registry.OccurrenceRegistry;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import org.joda.time.DateTime;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests whether the cassandra implementation doesn't affect the existing processing flow and comparison works as expected.
 * Note: This class should be removed as soon as the hazelcast / esper implementation is not a thing anymore.
 */
public class ShadowingTest {

    @Test
    public void newApproachExceptionDoesntBreakAnything() {
        Window window = new Window("whatever", new Quota(5, 1, TimeUnit.MINUTES, 100));
        OccurrenceRegistry registry = new OccurrenceRegistry() {
            @Override
            public void register(String userId, String messageId, Date receivedTime) {
                throw new RuntimeException("catch me");
            }

            @Override
            public int count(String userId, Date fromTime) {
                throw new RuntimeException("catch me");
            }
        };
        VolumeFilter filter = new VolumeFilter(mock(SharedBrain.class), mock(EventStreamProcessor.class),
                Collections.singleton(window), Duration.ofMillis(5), true, registry, 1) {
            @Override
            String extractSender(MessageProcessingContext context) {
                return "jdoe@example.com";
            }
        };
        MessageProcessingContext ctx = new MessageProcessingContext(mock(Mail.class), "messageId", mock(ProcessingTimeGuard.class)) {
            @Override
            public Message getMessage() {
                Message msg = mock(Message.class);
                when(msg.getReceivedAt()).thenReturn(DateTime.now());
                return msg;
            }
        };
        filter.filter(ctx);
    }

    // the idea is that the new, cassandra approach, may take a long time to complete, because of... whatever.
    // In this case, the new approach should be interrupted.
    // The test checks whether this behavior actually enforced.
    @Test(timeout = 5000L)
    public void newApproachTimeoutIsEnforced() {
        Window window = new Window("whatever", new Quota(5, 1, TimeUnit.MINUTES, 100));
        OccurrenceRegistry registry = new OccurrenceRegistry() {
            @Override
            public void register(String userId, String messageId, Date receivedTime) {
            }

            @Override
            public int count(String userId, Date fromTime) {
                try {
                    Thread.sleep(1_000_000_000_000L); // mysteriously, in a few days it will be exactly my age in milliseconds
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
                return 0;
            }
        };
        VolumeFilter filter = new VolumeFilter(mock(SharedBrain.class), mock(EventStreamProcessor.class),
                Collections.singleton(window), Duration.ofMillis(1), true, registry, 1) {
            @Override
            String extractSender(MessageProcessingContext context) {
                return "jdoe@example.com";
            }
        };
        MessageProcessingContext ctx = new MessageProcessingContext(mock(Mail.class), "messageId", mock(ProcessingTimeGuard.class)) {
            @Override
            public Message getMessage() {
                Message msg = mock(Message.class);
                when(msg.getReceivedAt()).thenReturn(DateTime.now());
                return msg;
            }
        };
        filter.filter(ctx);
        filter.filter(ctx); // the second attempt checks whether the first one is not blocked waiting for the cassandra implementation
    }
}
