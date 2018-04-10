package com.ecg.comaas.ebayk.listener.dailyreport.hadoop;

public class HadoopLogEntry {
    private final TrackerLogStyleUseCase logUseCase;
    private final String logEntry;

    public HadoopLogEntry(TrackerLogStyleUseCase logUseCase, String logEntry) {
        this.logUseCase = logUseCase;
        this.logEntry = logEntry;
    }

    final TrackerLogStyleUseCase getLogUseCase() {
        return logUseCase;
    }

    String getLogEntry() {
        return logEntry;
    }
}
