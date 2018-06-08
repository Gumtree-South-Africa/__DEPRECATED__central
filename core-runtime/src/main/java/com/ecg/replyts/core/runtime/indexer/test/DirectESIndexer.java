package com.ecg.replyts.core.runtime.indexer.test;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.indexer.IndexDataBuilder;
import com.ecg.replyts.core.runtime.indexer.MessageDocumentId;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * THIS CLASS SHOULD ONLY BE USED FOR TESTING
 * <p>
 * Indexes Conversations/Messages searches.
 * <b>Note:</b> The message type has to be setup specifically in order for the indexing to work properly. Please set up
 * your Elastic Search instance with the message_mapping.json file in the parent project.
 */
public class DirectESIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(DirectESIndexer.class);

    private static final String DOCUMENT_TYPE = "message";
    private static final String indexName = "replyts";

    private final Client elasticSearchClient;
    private final IndexDataBuilder indexDataBuilder;

    public DirectESIndexer(Client elasticSearchClient, IndexDataBuilder indexDataBuilder) {
        this.elasticSearchClient = elasticSearchClient;
        this.indexDataBuilder = indexDataBuilder;
    }

    public List<ListenableActionFuture<IndexResponse>> updateSearch(List<Conversation> conversations) throws IOException {
        List<ListenableActionFuture<IndexResponse>> futures = new ArrayList<>();
        for (Conversation conv : conversations) {
            futures.addAll(updateSearch(conv));
        }
        return futures;
    }

    public List<ListenableActionFuture<IndexResponse>> updateSearch(Conversation conversation) throws IOException {
        List<ListenableActionFuture<IndexResponse>> futures = new ArrayList<>();
        for (Message message : conversation.getMessages()) {
            XContentBuilder indexData = indexDataBuilder.toIndexData(conversation, message);

            IndexRequestBuilder indexRequestBuilder = elasticSearchClient
                    .prepareIndex(indexName, DOCUMENT_TYPE, getDocumentId(conversation, message))
                    .setSource(indexData);
            futures.add(indexRequestBuilder.execute());
        }
        return futures;
    }

    public void ensureIndexed(List<Conversation> conversation, int timeout, TimeUnit timeUnit) throws Exception {
        List<ListenableActionFuture<IndexResponse>> futures = updateSearch(conversation);
        waitFor(futures, timeout, timeUnit);
    }

    public void ensureIndexed(Conversation conversation, int timeout, TimeUnit timeUnit) throws Exception {
        List<ListenableActionFuture<IndexResponse>> futures = updateSearch(conversation);
        waitFor(futures, timeout, timeUnit);
    }

    private void waitFor(List<ListenableActionFuture<IndexResponse>> futures, int timeout, TimeUnit timeUnit) throws Exception {
        for (ListenableActionFuture<IndexResponse> future : futures) {
            IndexResponse indexResponse = future.get(timeout, timeUnit);
            if (indexResponse != null) {
                LOG.debug("Index response status: {}", indexResponse.status());
            }
        }
    }

    private String getDocumentId(Conversation conversation, Message message) {
        return new MessageDocumentId(conversation.getId(), message.getId()).build();
    }
}
