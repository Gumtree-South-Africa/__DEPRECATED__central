package com.ecg.comaas.mde.postprocessor.demandreporting;

import com.ecg.comaas.mde.postprocessor.demandreporting.domain.CommonEventData;
import com.ecg.comaas.mde.postprocessor.demandreporting.domain.EmailContactEvent;
import com.ecg.comaas.mde.postprocessor.demandreporting.domain.Event;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class BehaviorTrackingHandler {
    private static final Logger logger = LoggerFactory.getLogger(BehaviorTrackingHandler.class);
    private final Config config;
    private final EventPublisherFactory publisherFactory;
    private final LinkedBlockingQueue<Event> eventQueue;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean alreadyClosed = new AtomicBoolean(false);

    BehaviorTrackingHandler(Config config, EventPublisherFactory factory) {
        this.config = checkNotNull(config);
        this.publisherFactory = checkNotNull(factory);
        eventQueue = new LinkedBlockingQueue<>(config.getEventBufferSize() * 2);
        executor = Executors.newScheduledThreadPool(config.getThreadPoolSize());
        prepareEventPublishing();
    }

    private void prepareEventPublishing() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                flushOnExit();
            }
        });

        int initialDelay = config.getPublishingIntervalInSeconds() / config.getThreadPoolSize();
        for (int i = 0; i < config.getThreadPoolSize(); i++) {
            Runnable publisher = createEventPublisher();
            executor.scheduleAtFixedRate(publisher,
                    i * initialDelay,
                    config.getPublishingIntervalInSeconds(),
                    TimeUnit.SECONDS);
        }
    }

    private Runnable createEventPublisher() {
        return publisherFactory.create(eventQueue);
    }

    EmailContactEvent createTrackingEventFromContext(MessageProcessingContext context) {
        Message message = context.getMessage();
        CommonEventData commonEventData = CommonEventData.builder()
                .deviceId(getCustomVariableFromMessageOrContext("buyer_device_id", context))
                .userId(getCustomVariableFromMessageOrContext("buyer_customer_id", context))
                .ip(getTrackingHeaderFromMessage("Ip", message))
                .abTestsWithActiveVariants(Utils.parseAbTestMap(getTrackingHeaderFromMessage("Abtests", message)))
                .publisher(getCustomVariableFromMessageOrContext("publisher", context))
                .referrer(getTrackingHeaderFromMessage("Referrer", message))
                .txId(getTrackingHeaderFromMessage("Txid", message))
                .userAgent(getTrackingHeaderFromMessage("Useragent", message))
                .build();
        Mail mail = context.getMail().get();
        EmailContactEvent event = EmailContactEvent.builder()
                .adId(Utils.adIdStringToAdId(context.getConversation().getAdId()))
                .content(mail.getPlaintextParts().stream().collect(Collectors.joining("\n-------------------------------------------------\n")))
                .subject(mail.getSubject())
                .senderMailAddress(mail.getFrom())
                .receiverMailAddress(mail.getDeliveredTo())
                .replyToMailAddress(EmailAddressExctractor.extractFromSmptHeader(mail.getUniqueHeader("Reply-To"))) // none anonymized!
                .buildWithCommonEventData(commonEventData);
        return event;
    }

    private String getCustomVariableFromMessageOrContext(String name, MessageProcessingContext context) {
        return Optional.ofNullable(context.getMessage().getHeaders().get(String.format("X-Cust-%s", name.toUpperCase())))
                .orElse(context.getConversation().getCustomValues().get(name));
    }

    private String getTrackingHeaderFromMessage(String key, Message message) {
        return message.getHeaders().get(String.format("X-Track-%s", key));
    }

    void handle(MessageProcessingContext context) {
        try {
            Event entry = createTrackingEventFromContext(context);
            boolean trackingEnabled = config.isTrackingEnabled();
            boolean validForTracking = isEventValidForTracking(entry);
            if (trackingEnabled && validForTracking) {
                sendEvent(entry);
            }
        } catch (Exception e) {
            logger.error("Error while reporting behavior.", e);
        }
    }

    private boolean isEventValidForTracking(Event event) {
        return event != null
                && (event.getCommonEventData().getUserId() != null
                || event.getCommonEventData().getDeviceId() != null);
    }

    private void sendEvent(Event eventEntry) {
        if (!eventQueue.offer(eventEntry)) {
            logger.warn("Sending queue full - Skip event with type {} and txId {}",
                    eventEntry.getEventType(),
                    eventEntry.getCommonEventData().getTxId()
            );
        }
    }

    public void flushOnExit() {
        if (!alreadyClosed.getAndSet(true)) {
            executor.shutdown();
            createEventPublisher().run();
        }
    }

    public static class Config {
        @Value("${event.collector.service.url}")
        private String apiUrl;

        @Value("${mobile.event.tracking.enabled}")
        private boolean trackingEnabled;

        @Value("${mobile.event.tracking.pool.size}")
        private Integer threadPoolSize;

        @Value("${mobile.event.tracking.buffer.size}")
        private int eventBufferSize;

        @Value("${mobile.event.tracking.publishing.interval.in.seconds}")
        private int publishingIntervalInSeconds;

        public String getApiUrl() {
            return apiUrl;
        }

        public int getEventBufferSize() {
            return eventBufferSize;
        }

        public int getPublishingIntervalInSeconds() {
            return publishingIntervalInSeconds;
        }

        public Integer getThreadPoolSize() {
            return threadPoolSize;
        }

        public boolean isTrackingEnabled() {
            return trackingEnabled;
        }
    }
}
