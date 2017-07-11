package com.ecg.de.mobile.replyts.demand.usertracking;

import static com.ecg.de.mobile.replyts.demand.EmailAddressExctractor.extractFromSmptHeader;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.ecg.de.mobile.replyts.demand.Utils;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;


public class UserTrackingHandler {
    private static final Logger logger = LoggerFactory.getLogger(UserTrackingHandler.class);
    private final Config config;
    private final TrackingEventPublisherFactory publisherFactory;
    private final LinkedBlockingQueue<TrackingEvent> eventQueue;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean alreadyClosed = new AtomicBoolean(false);

    public UserTrackingHandler(Config config, TrackingEventPublisherFactory factory) {
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

        Long adId = Utils.adIdStringToAdId(context.getConversation().getAdId());

        EmailMessage msg = EmailMessage.builder()
            .ad(AdRef.of( adId, null))
            .ip(getTrackingHeaderFromMessage("Ip", message))
            .plainText(context.getMail().getPlaintextParts().stream().collect(Collectors.joining("\n-------------------------------------------------\n")))
            .subject(context.getMail().getSubject())
            .senderMailAddress(context.getOriginalFrom().getAddress())
            .receiverMailAddress(context.getOriginalTo().getAddress())
            .replyToMailAddress(extractFromSmptHeader(context.getMail().getUniqueHeader("Reply-To"))) // none anonymized!
            .build();
  
        ClientInfo ci = ClientInfo.builder()
                .withAkamaiBot(     getTrackingHeaderFromMessage("Akamaibot", message))
                .withApiVersion(    getTrackingHeaderFromMessage("Apiversion", message))
                .withDeviceType(    getTrackingHeaderFromMessage("Devicetype", message))
                .withIp(            getTrackingHeaderFromMessage("Ip", message))
                .withUserAgent(     getTrackingHeaderFromMessage("Useragent", message))
                .withVersion(       getTrackingHeaderFromMessage("Version", message))
                .withName(          getTrackingHeaderFromMessage("Appname", message))
                .build();
        
        Vi vi = Vi.of(
            getCustomVariableFromMessageOrContext("buyer_device_id", context),
            getCustomVariableFromMessageOrContext("buyer_customer_id", context)
        );

        return EmailContactEvent.builder()
            .txId(getTrackingHeaderFromMessage("Txid", message))
            .txSeq(getTrackingHeaderFromMessage("Txseq", message))
            .vi(vi)
            .ci(ci)
            .message(msg)
            .build();
    }

    private String getCustomVariableFromMessageOrContext(String name, MessageProcessingContext context) {
        String value = Optional.ofNullable(context.getMessage().getHeaders().get(String.format("X-Cust-%s", name.toUpperCase())))
                .orElse(context.getConversation().getCustomValues().get(name));
        return StringUtils.isNotBlank(value) ? value.trim() : null;
    }

    private String getTrackingHeaderFromMessage(String key, Message message) {
        return message.getHeaders().get(String.format("X-Track-%s", key));
    }

    public void handle(MessageProcessingContext context) {
        try {
            if (reportDemand(context)) {
                TrackingEvent emailContactEvent = createTrackingEventFromContext(context);
                boolean trackingEnabled = config.isTrackingEnabled();
                if (trackingEnabled) {
                    sendEvent(emailContactEvent);
                }
            }
        } catch (Exception e) {
            logger.error("Error while reporting tracking.", e);
        }
    }

    private static boolean reportDemand(MessageProcessingContext messageProcessingContext) {
        String demand = messageProcessingContext.getMessage().getHeaders().get("X-Report-Demand");
        return "true".equals(demand);
    }


    private void sendEvent(TrackingEvent eventEntry) {
        if (!eventQueue.offer(eventEntry)) {
            logger.warn("Sending queue full - Skip event with type {} and txId {}",
                        eventEntry.head.ns,
                        eventEntry.head.txId
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

        String getApiUrl() {
            return apiUrl;
        }

        int getEventBufferSize() {
            return eventBufferSize;
        }

        int getPublishingIntervalInSeconds() {
            return publishingIntervalInSeconds;
        }

        Integer getThreadPoolSize() {
            return threadPoolSize;
        }

        boolean isTrackingEnabled() {
            return trackingEnabled;
        }
    }



}
