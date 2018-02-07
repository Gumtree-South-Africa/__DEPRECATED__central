package com.ecg.replyts.core.runtime.persistence.kafka;

public class KafkaTopicService {
    // The topic for fresh and new incoming messages
    private static final String TOPIC_INCOMING = "_messages";

    // After some exception was caught, the message goes on this topic to retry
    private static final String TOPIC_RETRY = "_messages_retry";

    // A generic "I don't know what to do" topic
    private static final String TOPIC_FAILED = "_messages_failed";

    // If the message is not parseable, we won't retry. It goes to this topic instead
    private static final String TOPIC_UNPARSEABLE = "_messages_unparseable";

    // After n retries, put the message on this topic
    private static final String TOPIC_ABANDONED = "_messages_abandoned";

    public static String getTopicIncoming(String tenantShortName) {
        return prependTenant(tenantShortName, TOPIC_INCOMING);
    }

    public static String getTopicRetry(String tenantShortName){
        return prependTenant(tenantShortName, TOPIC_RETRY);
    }

    public static String getTopicFailed(String tenantShortName) {
        return prependTenant(tenantShortName, TOPIC_FAILED);
    }

    public static String getTopicUnparseable(String tenantShortName) {
        return prependTenant(tenantShortName, TOPIC_UNPARSEABLE);
    }

    public static String getTopicAbandoned(String tenantShortName) {
        return prependTenant(tenantShortName, TOPIC_ABANDONED);
    }

    private static String prependTenant(String tenantShortName, String topicName) {
        return tenantShortName + topicName;
    }
}
