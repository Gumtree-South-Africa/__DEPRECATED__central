package com.ecg.replyts.app.search.elasticsearch;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.search.MutableSearchService;
import com.ecg.replyts.core.api.search.RtsSearchGroupResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.indexer.MessageDocumentId;
import com.google.common.base.Preconditions;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.app.search.elasticsearch.SearchTransformer.translate;
import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

@Component
public class ElasticSearchSearchService implements SearchService, MutableSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchSearchService.class);

    private static final Counter MATCH_COUNTER = TimingReports.newCounter("es-match-counter");
    private static final Counter MISMATCH_COUNTER = TimingReports.newCounter("es-mismatch-counter");
    private static final Timer TOTAL_SEARCH_TIMER = TimingReports.newTimer("es-total-search");

    private static final Timer SEARCH_TIMER = TimingReports.newTimer("es-doSearch");
    private static final Timer GROUP_SEARCH_TIMER = TimingReports.newTimer("es-doGroupSearch");

    public static final String TYPE_NAME = "message";

    @Autowired
    @Qualifier("esclient")
    private Client client;

    @Autowired(required = false)
    @Qualifier("crossDCEsClient")
    private Client xDCclient;

    @Value("${search.es.indexname:replyts}")
    private String indexName;

    @Value("${search.es.timeout.ms:20000}")
    private long timeoutMs;

    @Override
    public RtsSearchResponse search(SearchMessagePayload searchMessageCommand) {
       try (Timer.Context timer = TOTAL_SEARCH_TIMER.time()) {
           RtsSearchResponse response = doSearch(xDCclient, indexName, searchMessageCommand);

           if (xDCclient != null) {
               RtsSearchResponse xDCresponse = doSearch(xDCclient, indexName, searchMessageCommand);
               if (response.equals(xDCresponse)) {
                   MATCH_COUNTER.inc();
               } else {
                   MISMATCH_COUNTER.inc();
                   LOG.info("Elastic search cross DC response differs from local Elastic. \nLocal: {}\n Remote {}", response, xDCresponse);
               }
           }
           return response;
       }
    }

    private RtsSearchResponse doSearch(Client client, String indexName, SearchMessagePayload searchMessagePayload) {
        SearchRequestBuilder searchRequestBuilder = translate(searchMessagePayload, client, indexName).intoQuery();

        LOG.trace("\n\nRequest:\n\n {}", searchRequestBuilder);

        SearchResponse searchResponse = executeSearch(searchRequestBuilder, SEARCH_TIMER);

        return createRtsSearchResponse(searchMessagePayload, searchResponse);
    }

    @Override
    public RtsSearchGroupResponse search(SearchMessageGroupPayload searchMessageCommand) {
        SearchRequestBuilder searchRequestBuilder = translate(searchMessageCommand, client, indexName).intoQuery();

        LOG.trace("\n\nRequest:\n\n {}", searchRequestBuilder);

        SearchResponse searchResponse = executeSearch(searchRequestBuilder, GROUP_SEARCH_TIMER);

        return createRtsSearchResponse(searchMessageCommand, searchResponse);
    }

    private SearchResponse executeSearch(SearchRequestBuilder searchRequestBuilder, Timer searchTimer) {
        try (Timer.Context ignore = searchTimer.time()) {
            SearchResponse response = searchRequestBuilder.execute().actionGet(timeoutMs, TimeUnit.MILLISECONDS);

            return response;
        } catch (Exception e) {
            throw new RuntimeException(format("Couldn't perform search query %s", searchRequestBuilder), e);
        }
    }

    private RtsSearchResponse createRtsSearchResponse(SearchMessagePayload command, SearchResponse response) {
        SearchHit[] hits = response.getHits().getHits();
        List<RtsSearchResponse.IDHolder> ids = new ArrayList<>(hits.length);

        extractMsgDocIdsFromHits(hits, ids);

        return new RtsSearchResponse(ids, command.getOffset(), hits.length, (int) response.getHits().getTotalHits());
    }

    private RtsSearchGroupResponse createRtsSearchResponse(SearchMessageGroupPayload command, SearchResponse response) {
        Terms topLevelAgg = response.getAggregations().get(SearchTransformer.GROUPING_AGG_NAME);
        List<Terms.Bucket> buckets = topLevelAgg.getBuckets();
        Map<String, RtsSearchResponse> messageGroups = new LinkedHashMap<>(buckets.size());
        for (Terms.Bucket bucket : buckets) {
            TopHits itemsAgg = bucket.getAggregations().get(SearchTransformer.ITEMS_AGG_NAME);

            SearchHits hits = itemsAgg.getHits();
            int numHits = hits.getHits().length;
            List<RtsSearchResponse.IDHolder> ids = new ArrayList<>(numHits);

            extractMsgDocIdsFromHits(hits.getHits(), ids);

            messageGroups.put(bucket.getKey(), new RtsSearchResponse(ids, command.getOffset(), numHits, (int) hits.getTotalHits()));
        }

        // Grouping searches return only one page of results
        return new RtsSearchGroupResponse(messageGroups);
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

        client.prepareDeleteByQuery(indexName)
                .setTypes(TYPE_NAME)
                .setQuery(
                        rangeQuery("lastModified")
                                .from(from)
                                .to(to))
                .execute()
                .actionGet();
    }
}