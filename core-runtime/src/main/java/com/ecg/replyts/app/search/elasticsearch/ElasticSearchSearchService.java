package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.search.MutableSearchService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.indexer.MessageDocumentId;
import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class ElasticSearchSearchService implements SearchService, MutableSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchSearchService.class);

    private final ElasticQueryFactory queryFactory;
    private final TimeValue searchTimeout;

    ElasticSearchSearchService(ElasticQueryFactory queryFactory, TimeValue searchTimeout) {
        this.queryFactory = queryFactory;
        this.searchTimeout = searchTimeout;
    }

    public RtsSearchResponse search(SearchMessagePayload payload) {
        SearchRequestBuilder request = queryFactory.searchMessage(payload);
        LOG.trace("\n\nRequest:\n\n {}", request);
        SearchResponse response = executeSearch(request);
        return createRtsSearchResponse(payload, response);
    }

    private SearchResponse executeSearch(SearchRequestBuilder request) {
        try {
            SearchResponse response = request.execute().actionGet(searchTimeout);
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

        queryFactory.deleteLastModified(from, to).get();
        queryFactory.deleteReceivedDate(from, to).get();
    }
}