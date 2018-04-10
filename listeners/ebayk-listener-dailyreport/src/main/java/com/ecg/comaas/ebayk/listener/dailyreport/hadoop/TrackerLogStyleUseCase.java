package com.ecg.comaas.ebayk.listener.dailyreport.hadoop;

public enum TrackerLogStyleUseCase {
    MESSAGE_EVENTS_V3("hadoop-message-events-v3-json"),
    DAILY_REPORT_V3("hadoop-daily-report-v3-json"),
    FILTER_PERFORMANCE_V2("hadoop-filter-performance-v2-json");

    private final String topicName;

    public String getTopicName() {
        return topicName;
    }

    TrackerLogStyleUseCase(String topicName) {
        this.topicName = topicName;
    }
}
