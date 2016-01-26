package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.indexer.IndexerStatus;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.net.InetAddress.getLocalHost;

class IndexingJournal {

    private final Map<IndexingMode, DataPayload> distributedMap;
    private final IndexingMode indexingMode;

    IndexingJournal(Map<IndexingMode, DataPayload> distributedMap, DateTime startTime, DateTime endTime, IndexingMode indexingMode) {
        this.distributedMap = distributedMap;
        this.indexingMode = indexingMode;
        this.distributedMap.put(indexingMode, new DataPayload(startTime, endTime, indexingMode));
    }

    public void startRunning(int totalChunkCount) {
        DataPayload dataPayload = get();
        try {
            dataPayload.setHostname(getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        dataPayload.setStartedRunning(DateTime.now());
        dataPayload.setChunkCount(totalChunkCount);

        put(dataPayload);
    }

    public void completeRunning() {
        DataPayload dataPayload = get();

        dataPayload.setEndedRunning(DateTime.now());

        put(dataPayload);
    }

    public void completedChunk() {
        DataPayload dataPayload = get();
        dataPayload.finishedChunk();
        put(dataPayload);
    }

    private DataPayload put(DataPayload dataPayload) {
        return distributedMap.put(indexingMode, dataPayload);
    }

    private DataPayload get() {
        return this.distributedMap.get(indexingMode);
    }

    public static class DataPayload implements Serializable {

        private final DateTime startTimeRange, endTimeRange;
        private final String indexingType;
        private DateTime startedRunning;
        private DateTime endedRunning;
        private String hostname;
        private int chunkCount;
        private final AtomicInteger chunksProcessed = new AtomicInteger(0);

        DataPayload(DateTime startTimeRange, DateTime endTimeRange, IndexingMode indexingType) {
            this.startTimeRange = startTimeRange;
            this.endTimeRange = endTimeRange;
            this.indexingType = indexingType.name();
        }

        void setStartedRunning(DateTime startedRunning) {
            this.startedRunning = startedRunning;
        }

        void setEndedRunning(DateTime endedRunning) {
            this.endedRunning = endedRunning;
        }

        void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        void finishedChunk() {
            chunksProcessed.incrementAndGet();
        }

        public IndexerStatus toIndexerStatus() {
            IndexerStatus status = new IndexerStatus();
            status.setCompletedChunks(chunksProcessed.get());
            status.setTotalChunks(chunkCount);
            status.setRunning(endedRunning == null);
            status.setDateTo(endTimeRange);
            status.setDateFrom(startTimeRange);
            status.setMode(indexingType);
            status.setStartDate(startedRunning);
            status.setEndDate(endedRunning);
            status.setHostName(hostname);
            return status;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }
}
