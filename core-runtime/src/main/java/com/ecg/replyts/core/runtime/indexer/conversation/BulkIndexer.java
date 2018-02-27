package com.ecg.replyts.core.runtime.indexer.conversation;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Iterables;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BulkIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(BulkIndexer.class);

    private static final Counter INDEX_OPERATIONS_COUNTER = TimingReports.newCounter("streaming.indexer.es-index-documents");
    private static final Counter BULK_REQUESTS_SUBMITTED_COUNTER = TimingReports.newCounter("streaming.indexer.es-bulk-index-submitted");
    private static final Counter BULK_REQUESTS_PROCESSED_COUNTER = TimingReports.newCounter("streaming.indexer.es-bulk-index-processed");
    private static final Counter DOCUMENTS_PROCESSED_COUNTER = TimingReports.newCounter("streaming.indexer.es-bulk-documents-processed");
    private static final Counter BULK_REQUESTS_FAILURES_COUNTER = TimingReports.newCounter("streaming.indexer.es-bulk-index-failures");
    private static final Histogram BULK_SIZE_BYTES = TimingReports.newHistogram("streaming.indexer.es-bulk-size-bytes");
    private static final Timer BULK_RESPONSE_TIMER = TimingReports.newTimer("streaming.indexer.es-bulk-time-ms");

    private final Client elasticSearchClient;
    private final IndexDataBuilder indexDataBuilder;

    @Value("${search.es.indexname:replyts}")
    private String indexName = "replyts";

    @Value("${search.es.enabled:true}")
    private boolean esEnabled = true;

    private BulkProcessor bulkProcessor;

    public BulkIndexer(Client elasticSearchClient, IndexDataBuilder indexDataBuilder, int concurrency, int batchSizeToFlushMb, int maxActions) {
        this.elasticSearchClient = elasticSearchClient;
        this.indexDataBuilder = indexDataBuilder;
        this.bulkProcessor = prepareBulk(concurrency, batchSizeToFlushMb, maxActions);
    }

    // Alternatively we could handle that in Grafana
    public void resetCounters() {
        INDEX_OPERATIONS_COUNTER.dec(INDEX_OPERATIONS_COUNTER.getCount());
        BULK_REQUESTS_SUBMITTED_COUNTER.dec(BULK_REQUESTS_SUBMITTED_COUNTER.getCount());
        BULK_REQUESTS_PROCESSED_COUNTER.dec(BULK_REQUESTS_PROCESSED_COUNTER.getCount());
        DOCUMENTS_PROCESSED_COUNTER.dec(DOCUMENTS_PROCESSED_COUNTER.getCount());
        BULK_REQUESTS_FAILURES_COUNTER.dec(DOCUMENTS_PROCESSED_COUNTER.getCount());
    }

    private BulkProcessor prepareBulk(int concurrency, int batchSizeToFlushMb, int maxActions) {
        LOG.debug("Preparing bulk processor with concurrency {}, bulk flush size {} and maxActions {}", concurrency, batchSizeToFlushMb, maxActions);
        return BulkProcessor.builder(
                elasticSearchClient,
                new BulkProcessor.Listener() {
                    @Override
                    public void beforeBulk(long executionId,
                                           BulkRequest request) {
                        BULK_REQUESTS_SUBMITTED_COUNTER.inc();
                        LOG.debug("About to submit batch {} with {} actions and approximate size of {} bytes ", executionId, request.numberOfActions(), request.estimatedSizeInBytes());
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          BulkResponse response) {
                        BULK_REQUESTS_PROCESSED_COUNTER.inc();
                        DOCUMENTS_PROCESSED_COUNTER.inc(request.numberOfActions());
                        BULK_SIZE_BYTES.update(request.estimatedSizeInBytes());
                        BULK_RESPONSE_TIMER.update(response.getTookInMillis(), TimeUnit.MILLISECONDS);
                        LOG.debug("Batch {} processing completed successfully", executionId);
                    }

                    @Override
                    public void afterBulk(long executionId,
                                          BulkRequest request,
                                          Throwable failure) {
                        BULK_REQUESTS_FAILURES_COUNTER.inc();
                        LOG.error("Bulk indexing failed with {} for task {}. Request: {} ", executionId, failure.getMessage(), request.toString(), failure);
                    }
                })
                .setBulkActions(maxActions)
                .setBulkSize(new ByteSizeValue(batchSizeToFlushMb, ByteSizeUnit.MB))
                .setConcurrentRequests(concurrency)
                .build();
    }

    public static long getFetchedDocumentCount() {
        return INDEX_OPERATIONS_COUNTER.getCount();
    }

    public void updateIndex(List<Conversation> conversations) {
        if (!esEnabled) {
            return;
        }
        for (Conversation conversation : conversations) {
            updateIndex(conversation);
        }
        Conversation lastProcessedConversation = Iterables.getLast(conversations);
        LOG.info("Last processed conversation - {}, createdAt - {}, lastModifiedAt - {}",
                lastProcessedConversation.getId(), lastProcessedConversation.getCreatedAt(), lastProcessedConversation.getLastModifiedAt());
    }

    public void updateIndex(Conversation conversation) {
        if (conversation == null || conversation.getMessages().isEmpty()) {
            return;
        }

        INDEX_OPERATIONS_COUNTER.inc(conversation.getMessages().size());
        try {
            for (Message message : conversation.getMessages()) {
                IndexData indexData = indexDataBuilder.toIndexData(conversation, message);

                IndexRequestBuilder indexRequestBuilder = elasticSearchClient
                        .prepareIndex(indexName, IndexData.DOCUMENT_TYPE, indexData.getDocumentId())
                        .setSource(indexData.getDocument());
                bulkProcessor.add(indexRequestBuilder.request());
            }
        } catch (IOException e) {
            LOG.error("Failed to add conversation {} to index", conversation.getId(), e);
        }

    }

    public void flush() {
        bulkProcessor.flush();
    }

    /**
     * @return Returns: true if all bulk requests completed and false if the waiting time elapsed before all the bulk requests completed
     */
    @PreDestroy
    public boolean awaitClose() {
        try {
            return bulkProcessor.awaitClose(4, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            LOG.warn("The ES Bulk process has been interrupted");
            Thread.currentThread().interrupt();
            return false;
        }
    }

}