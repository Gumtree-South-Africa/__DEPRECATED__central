package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.tophits.TopHitsBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryString;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;

public abstract class SearchTransformer {
    private static final int GROUP_SIZE = 10;

    public static final String GROUPING_AGG_NAME = "grouping";
    public static final String ITEMS_AGG_NAME = "items";

    final SearchMessagePayload payload;
    private final Client client;
    private final String indexName;
    private Optional<BoolQueryBuilder> rootQuery = Optional.empty();
    private Optional<AndFilterBuilder> rootFilter = Optional.empty();

    private final boolean groupedSearch;
    private final TermsBuilder topLevelFieldAggregation = AggregationBuilders.terms(GROUPING_AGG_NAME);
    private final TopHitsBuilder itemsAggregation = AggregationBuilders.topHits(ITEMS_AGG_NAME);

    SearchTransformer(SearchMessagePayload payload, Client client, String indexName) {
        this.payload = payload;
        this.client = client;
        this.indexName = indexName;
        this.groupedSearch = false;
    }

    SearchTransformer(SearchMessageGroupPayload payload, Client client, String indexName) {
        this.payload = payload;
        this.client = client;
        this.indexName = indexName;
        this.groupedSearch = true;
    }

    /**
     * analyzes the given payload and builds an ES request out of it
     */
    public SearchRequestBuilder intoQuery() {
        SearchRequestBuilder searchRequestBuilder = setupSearchRequestBuilder();

        setupTimeRangeFilter();
        setupQueryForFilterRuleHitsFilter();
        setupMessageStateFilters();
        setupAdIdFilter();
        setupEditorFilter();
        setupAttachmentsFilter();
        setupSearchSpecificParameters();
        setupAggregations(searchRequestBuilder);
        setupOrdering(searchRequestBuilder);
        setupPaging(searchRequestBuilder);

        return finaliseSearchRequestBuilder(searchRequestBuilder);
    }

    abstract void setupSearchSpecificParameters();

    void addFilter(FilterBuilder filter) {
        if (!rootFilter.isPresent()) {
            rootFilter = Optional.of(FilterBuilders.andFilter());
        }
        rootFilter.get().add(filter);
    }

    void addQuery(QueryBuilder query) {
        if (!rootQuery.isPresent()) {
            rootQuery = Optional.of(boolQuery());
        }
        rootQuery.get().must(query);
    }

    private SearchRequestBuilder finaliseSearchRequestBuilder(SearchRequestBuilder searchRequestBuilder) {
        if (groupedSearch || payload.isUseFilterQuery()) {
            searchRequestBuilder.setQuery(
              QueryBuilders.filteredQuery(rootQuery.orElse(null), rootFilter.orElse(null))
            );
        } else {
            if (rootQuery.isPresent()) {
                searchRequestBuilder.setQuery(rootQuery.get()); // rootQuery.get().buildAsBytes() - removed. does not make any sense to me
            }
            if (rootFilter.isPresent()) {
                searchRequestBuilder.setPostFilter(rootFilter.get());
            }
        }
        return searchRequestBuilder;
    }

    private SearchRequestBuilder setupSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName).setTypes(ElasticSearchSearchService.TYPE_NAME);
        if (groupedSearch) {
            searchRequestBuilder = searchRequestBuilder
                    .setSearchType(SearchType.COUNT) // we only care about the aggregation hits.
                    // also, query cache only works for this type
                    .setQueryCache(true);
        } else {
            searchRequestBuilder = searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        }
        return searchRequestBuilder;
    }

    private void setupAttachmentsFilter() {
        if (!StringUtils.hasText(payload.getAttachments())) {
            return;
        }

        FilterBuilder forAttachments = queryFilter(queryString(payload.getAttachments()).defaultField("attachments"));
        addFilter(forAttachments);

    }

    private void setupEditorFilter() {
        String lastEditor = payload.getLastEditor();
        if (lastEditor != null) {
            addFilter(termFilter("lastEditor", lastEditor));
        }
    }

    private void setupAdIdFilter() {
        if (StringUtils.hasText(payload.getAdId())) {
            TermFilterBuilder adIdFilter = termFilter("adId", payload.getAdId());
            addFilter(adIdFilter);
        }
    }

    private void setupMessageStateFilters() {
        if (payload.getMessageStates() != null) {
            TermsFilterBuilder messageStateFilterBuilder = FilterBuilders.inFilter("messageState", payload.getMessageStates().stream().map(Enum::name).toArray());
            addFilter(messageStateFilterBuilder);
        }
        if (payload.getHumanResultState() != null) {
            TermFilterBuilder humanResultStateFilter = termFilter("humanResultState", payload.getHumanResultState().name());
            addFilter(humanResultStateFilter);
        }
    }

    private void setupQueryForFilterRuleHitsFilter() {
        if (StringUtils.hasText(payload.getFilterName())) {
            TermFilterBuilder filterNameFilter = termFilter("filterName", payload.getFilterName());
            addFilter(filterNameFilter);
        }

        if (StringUtils.hasText(payload.getFilterInstance())) {
            TermFilterBuilder filterInstanceFilter = termFilter("filterInstance", payload.getFilterInstance());
            addFilter(filterInstanceFilter);
        }
    }

    private void setupTimeRangeFilter() {
        if (payload.getFromDate() != null || payload.getToDate() != null) {
            RangeFilterBuilder rangeFilter = rangeFilter("receivedDate");
            rangeFilter.from(payload.getFromDate());
            rangeFilter.to(payload.getToDate());
            addFilter(rangeFilter);
        }
    }

    private void setupOrdering(SearchRequestBuilder searchRequestBuilder) {
        SortOrder so;
        switch (payload.getOrdering()) {
            case NEWEST_FIRST:
                so = DESC;
                break;
            case OLDEST_FIRST:
                so = ASC;
                break;
            default:
                throw new IllegalStateException("can not understand ordering " + payload.getOrdering());
        }

        if (!groupedSearch) {
            searchRequestBuilder.addSort("receivedDate", so);
        }

        itemsAggregation.addSort("receivedDate", so);
    }

    private void setupPaging(SearchRequestBuilder searchRequestBuilder) {
        // setSize 0 and setOffset 0 will return in zero results
        if (payload.getCount() > 0) {
            if (!groupedSearch) {
                searchRequestBuilder.setSize(payload.getCount());
            }
            topLevelFieldAggregation.size(payload.getCount());
            // Reduce the likelihood of errors in the aggregation
            // See http://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-terms-aggregation.html
            topLevelFieldAggregation.shardSize(payload.getCount() * 2);
        }
        if (payload.getOffset() > 0) {
            searchRequestBuilder.setFrom(payload.getOffset());
            itemsAggregation.setFrom(payload.getOffset());
        }

        // Fixed number of items per group for now
        itemsAggregation.setSize(GROUP_SIZE);
    }

    private void setupAggregations(SearchRequestBuilder searchRequestBuilder) {
        if (!groupedSearch) {
            return;
        }

        topLevelFieldAggregation
                .field(((SearchMessageGroupPayload) payload).getGroupBy())
                .subAggregation(itemsAggregation);

        searchRequestBuilder.addAggregation(topLevelFieldAggregation);
    }
}