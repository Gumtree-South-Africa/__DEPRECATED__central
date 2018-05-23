package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.search.MutableSearchService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.indexer.MessageDocumentId;
import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

public class ElasticSearchService implements SearchService, MutableSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchService.class);

    private final RestHighLevelClient client;
    private final ElasticDeleteClient deleteClient;
    private final String indexName;

    ElasticSearchService(RestHighLevelClient client, ElasticDeleteClient deleteClient, String indexName) {
        this.client = client;
        this.deleteClient = deleteClient;
        this.indexName = indexName;
    }

    public RtsSearchResponse search(SearchMessagePayload payload) {
        SearchRequest request = new SearchTransformer(payload, indexName).intoQuery();
        LOG.trace("\n\nRequest:\n\n {}", request);
        SearchResponse response = executeSearch(request);
        return createRtsSearchResponse(payload, response);
    }

    private SearchResponse executeSearch(SearchRequest request) {
        try {
            SearchResponse response = client.search(request);
            LOG.trace("\n\nResponse:\n\n{}", response);
            return response;
        } catch (Exception e) {
            throw new RuntimeException(format("Couldn't perform search query %s", request), e);
        }
    }

    private RtsSearchResponse createRtsSearchResponse(SearchMessagePayload command, SearchResponse response) {
        SearchHit[] hits = response.getHits().getHits();
        List<RtsSearchResponse.IDHolder> ids = new ArrayList<>(hits.length);

        extractMsgDocIdsFromHits(hits, ids);

        return new RtsSearchResponse(ids, command.getOffset(), hits.length, (int) response.getHits().getTotalHits());
    }

    private void extractMsgDocIdsFromHits(SearchHit[] hits, List<RtsSearchResponse.IDHolder> storedIds) {
        for (SearchHit hit : hits) {
            try {
                MessageDocumentId messageDocumentId = MessageDocumentId.parse(hit.getId());

                storedIds.add(new RtsSearchResponse.IDHolder(messageDocumentId.getMessageId(), messageDocumentId.getConversationId()));
            } catch (RuntimeException exception) {
                LOG.error("Could not extract message/conv from searchresult: {} ", hit.getId(), exception);
            }
        }
    }

    @Override
    public void deleteModifiedAt(LocalDate from, LocalDate to) {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);

        deleteLastModified(from, to);
        deleteReceivedDate(from, to);
    }

    void deleteLastModified(LocalDate from, LocalDate to) {
        RangeQueryBuilder lastModified = rangeQuery("lastModified")
                .from(from)
                .to(to);

        String query = new SearchSourceBuilder()
                .query(lastModified)
                .toString();

        deleteClient.delete(query);
    }

    void deleteReceivedDate(LocalDate from, LocalDate to) {
        RangeQueryBuilder receivedDateQuery = rangeQuery("receivedDate")
                .from(from)
                .to(to);

        BoolQueryBuilder lastModified = QueryBuilders.boolQuery()
                .filter(receivedDateQuery)
                .mustNot(QueryBuilders.existsQuery("lastModified"));

        String query = new SearchSourceBuilder()
                .query(lastModified)
                .toString();

        deleteClient.delete(query);
    }
}