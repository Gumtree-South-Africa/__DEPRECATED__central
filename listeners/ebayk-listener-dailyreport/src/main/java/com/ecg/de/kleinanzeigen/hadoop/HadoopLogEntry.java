package com.ecg.de.kleinanzeigen.hadoop;

/**
 * Created by johndavis on 29/11/16.
 */
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
