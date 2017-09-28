package com.ecg.replyts.core.runtime.indexer.conversation;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.TimingReports;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.*;

/**
 * Indexes Conversations/Messages searches.
 * <b>Note:</b> The message type has to be setup specifically in order for the indexing to work properly. Please set up
 * your Elastic Search instance with the message_mapping.json file in the parent project.
 */
public class SearchIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(SearchIndexer.class);
    private static final long TIMEOUT_SYNC_UPDATE_MINUTES = 10L;
    private static final Counter INDEX_OPERATIONS_COUNTER = TimingReports.newCounter("es-index-documents");

    private final Client elasticSearchClient;
    private final IndexDataBuilder indexDataBuilder;

    @Value("${search.es.indexname:replyts}")
    private String indexName = "replyts";

    @Value("${search.es.enabled:true}")
    private boolean esEnabled = true;

    public SearchIndexer(Client elasticSearchClient, IndexDataBuilder indexDataBuilder) {
        this.elasticSearchClient = elasticSearchClient;
        this.indexDataBuilder = indexDataBuilder;
    }

    public void updateSearchSync(List<Conversation> conversations) {
        if (!esEnabled) {
            return;
        }
        try {
            ListenableActionFuture<BulkResponse> responseFuture = updateSearchAsync(conversations);
            BulkResponse response = responseFuture.get(TIMEOUT_SYNC_UPDATE_MINUTES, MINUTES);
            if (response.hasFailures()) {
                throw new RuntimeException(response.buildFailureMessage());
            }
        } catch (InterruptedException e) {
            LOG.warn("the thread has been interrupted");
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public ListenableActionFuture<BulkResponse> updateSearchAsync(List<Conversation> conversations) {
        return updateSearchAsync(conversations, true);
    }

    public ListenableActionFuture<BulkResponse> updateSearchAsync(List<Conversation> conversations, boolean registerListener) {
        if (!esEnabled) {
            return null;
        }
        try {
            BulkRequestBuilder bulk = elasticSearchClient.prepareBulk();
            for (Conversation conversation : conversations) {
                updateSearch(conversation, bulk);
            }
            ListenableActionFuture<BulkResponse> execute = bulk.execute();

            if (registerListener) {
                ActionListener<BulkResponse> listener = new ActionListener<BulkResponse>() {
                    @Override
                    public void onResponse(BulkResponse bulkItemResponses) {
                        LOG.trace("Indexing complete");
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        StringBuilder s = new StringBuilder("Failed Indexing conversations: '");
                        for (Conversation conversation : conversations) {
                            s.append(conversation.getId()).append(", ");
                        }
                        s.append("'");
                        LOG.error(s.toString(), e);
                    }
                };
                execute.addListener(listener);
            }
            return execute;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateSearch(Conversation conversation, BulkRequestBuilder bulk) throws IOException {
        INDEX_OPERATIONS_COUNTER.inc(conversation.getMessages().size());
        for (Message message : conversation.getMessages()) {
            IndexData indexData = indexDataBuilder.toIndexData(conversation, message);

            IndexRequestBuilder indexRequestBuilder = elasticSearchClient
                    .prepareIndex(indexName, IndexData.DOCUMENT_TYPE, indexData.getDocumentId())
                    .setSource(indexData.getDocument());
            bulk.add(indexRequestBuilder);
        }
    }
}