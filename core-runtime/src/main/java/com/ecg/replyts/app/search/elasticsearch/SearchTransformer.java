package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.google.common.base.Strings;
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

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.elasticsearch.search.sort.SortOrder.DESC;

/**
 * Takes a {@link com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload}
 * or a {@link com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload}
 * and converts it into a query that ES can understand.
 *
 * @author mhuttar
 */
final class SearchTransformer {

    private static final int GROUP_SIZE = 10;

    public static final String GROUPING_AGG_NAME = "grouping";
    public static final String ITEMS_AGG_NAME = "items";

    private final SearchMessagePayload payload;
    private final Client client;
    private final String indexName;
    private Optional<BoolQueryBuilder> rootQuery = Optional.empty();

    private Optional<AndFilterBuilder> rootFilter = Optional.empty();
    private final boolean groupedSearch;
    private final TermsBuilder topLevelFieldAggregation = AggregationBuilders.terms(GROUPING_AGG_NAME);
    private final TopHitsBuilder itemsAggregation = AggregationBuilders.topHits(ITEMS_AGG_NAME);

    private SearchTransformer(SearchMessagePayload payload, Client client, String indexName) {
        this.payload = payload;
        this.client = client;
        this.indexName = indexName;
        this.groupedSearch = false;
    }

    private SearchTransformer(SearchMessageGroupPayload payload, Client client, String indexName) {
        this.payload = payload;
        this.client = client;
        this.indexName = indexName;
        this.groupedSearch = true;
    }

    /**
     * analyzes the given payload and builds an ES request out of it
     */
    public SearchRequestBuilder intoQuery() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(indexName).setTypes(ElasticSearchSearchService.TYPE_NAME);
        if (groupedSearch) {
            searchRequestBuilder = searchRequestBuilder
                    .setSearchType(SearchType.COUNT) // we only care about the aggregation hits.
                            // also, query cache only works for this type
                    .setQueryCache(true);
        } else {
            searchRequestBuilder = searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        }


        setupTimeRangeFilter();

        setupQueryForFilterRuleHitsFilter();

        setupMessageStateFilters();

        setupAdIdFilter();

        setupMessageFreetextQuery();

        setupMailAddressQuery();

        setupEditorFilter();

        setupCustomValuesQuery();

        setupAttachmentsFilter();

        setupAggregations(searchRequestBuilder);

        setupOrdering(searchRequestBuilder);

        setupPaging(searchRequestBuilder);

        searchRequestBuilder.setQuery(filteredQuery(rootQuery.orElse(null), rootFilter.orElse(null)));

        return searchRequestBuilder;
    }

    private void setupAttachmentsFilter() {
        if (Strings.isNullOrEmpty(payload.getAttachments())) {
            return;
        }
        FilterBuilder forAttachments = queryFilter(queryString(payload.getAttachments()).defaultField("attachments"));
        addFilter(forAttachments);
    }

    private void setupEditorFilter() {
        Optional<String> lastEditor = ofNullable(payload.getLastEditor());
        lastEditor.ifPresent(value -> {
            TermFilterBuilder adIdFilter = termFilter("lastEditor", value);
            addFilter(adIdFilter);
        });
    }

    private void setupCustomValuesQuery() {
        if (!payload.getConversationCustomValues().isEmpty()) {
            BoolQueryBuilder andQuery = boolQuery();
            for (String key : payload.getConversationCustomValues().keySet()) {
                //by convention headers are always lowercased
                andQuery.must(matchPhraseQuery("customHeaders." + key.toLowerCase(),
                        payload.getConversationCustomValues().get(key)));
            }
            addQuery(andQuery);
        }
    }

    private void setupMailAddressQuery() {
        if (StringUtils.hasText(payload.getUserEmail())) {
            String userEmailLowerCase = payload.getUserEmail().toLowerCase();
            switch (payload.getUserRole()) {
                case RECEIVER:
                    addQuery(wildcardQuery("toEmail", userEmailLowerCase));
                    break;
                case RECEIVER_ANONYMOUS:
                    addQuery(wildcardQuery("toEmailAnonymous", userEmailLowerCase));
                    break;
                case SENDER:
                    addQuery(wildcardQuery("fromEmail", userEmailLowerCase));
                    break;
                case SENDER_ANONYMOUS:
                    addQuery(wildcardQuery("fromEmailAnonymous", userEmailLowerCase));
                    break;
                case ANY:
                    BoolQueryBuilder orQuery = boolQuery();
                    orQuery.should(wildcardQuery("fromEmail", userEmailLowerCase));
                    orQuery.should(wildcardQuery("toEmail", userEmailLowerCase));
                    orQuery.should(wildcardQuery("fromEmailAnonymous", userEmailLowerCase));
                    orQuery.should(wildcardQuery("toEmailAnonymous", userEmailLowerCase));
                    addQuery(orQuery);
                    break;
                default:
                    throw new IllegalStateException("Unknown enumeration value: " + payload.getUserRole().name());
            }
        }
    }

    private void setupMessageFreetextQuery() {
        if (StringUtils.hasText(payload.getMessageTextKeywords())) {
            addQuery(new QueryStringQueryBuilder(payload.getMessageTextKeywords()).field("messageText"));
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

    private void addFilter(FilterBuilder filter) {
        if (!rootFilter.isPresent()) {
            rootFilter = of(FilterBuilders.andFilter());
        }
        rootFilter.get().add(filter);
    }

    private void addQuery(QueryBuilder query) {
        if (!rootQuery.isPresent()) {
            rootQuery = of(boolQuery());
        }
        rootQuery.get().must(query);
    }


    /**
     * creates a new transformer that is capable of transforming the given search into a query that ES understands.
     */
    static SearchTransformer translate(SearchMessagePayload payload, Client client, String indexName) {
        return new SearchTransformer(payload, client, indexName);
    }

    static SearchTransformer translate(SearchMessageGroupPayload payload, Client client, String indexName) {
        return new SearchTransformer(payload, client, indexName);
    }
}
