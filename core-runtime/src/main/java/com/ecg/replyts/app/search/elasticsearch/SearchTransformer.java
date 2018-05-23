package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.google.common.base.Strings;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static org.elasticsearch.index.query.QueryBuilders.*;

class SearchTransformer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private final SearchMessagePayload payload;
    private final String indexName;

    private BoolQueryBuilder rootBoolQuery = QueryBuilders.boolQuery();

    SearchTransformer(SearchMessagePayload payload, String indexName) {
        this.payload = payload;
        this.indexName = indexName;
    }

    SearchRequest intoQuery() {
        SearchSourceBuilder requestBuilder = new SearchSourceBuilder();
        setupOrdering(requestBuilder);
        setupTimeRangeFilter();
        setupQueryForFilterRuleHitsFilter();
        setupMessageStateFilters();
        setupAdIdFilter();
        setupMessageFreetextQuery();
        setupMailAddressQuery();
        setupEditorFilter();
        setupCustomValuesQuery();
        setupAttachmentsFilter();
        setupPaging(requestBuilder);
        requestBuilder.query(rootBoolQuery);

        return new SearchRequest()
                .indices(indexName)
                .types("message")
                .searchType(SearchType.DFS_QUERY_THEN_FETCH)
                .source(requestBuilder);
    }

    private void setupOrdering(SearchSourceBuilder request) {
        SortOrder so = payload.getOrdering().equals(SearchMessagePayload.ResultOrdering.NEWEST_FIRST) ? SortOrder.DESC : SortOrder.ASC;
        request.sort("receivedDate", so);
    }

    private void setupAttachmentsFilter() {
        if (!Strings.isNullOrEmpty(payload.getAttachments())) {
            QueryStringQueryBuilder filter = queryStringQuery(payload.getAttachments()).defaultField("attachments");
            rootBoolQuery.filter(filter);
        }
    }

    private void setupEditorFilter() {
        if (payload.getLastEditor() != null) {
            TermsQueryBuilder query = QueryBuilders.termsQuery("lastEditor", payload.getLastEditor());
            rootBoolQuery.filter(query);
        }
    }

    private void setupCustomValuesQuery() {
        if (!payload.getConversationCustomValues().isEmpty()) {
            BoolQueryBuilder andQuery = boolQuery();

            for (String key : payload.getConversationCustomValues().keySet()) {
                // by convention headers are always lowercase
                andQuery.must(matchPhraseQuery("customHeaders." + key.toLowerCase(), payload.getConversationCustomValues().get(key)));
            }

            rootBoolQuery.must(andQuery);
        }
    }

    private void setupMailAddressQuery() {
        if (!Strings.isNullOrEmpty(payload.getUserEmail())) {
            String userEmailLowerCase = payload.getUserEmail().toLowerCase();

            switch (payload.getUserRole()) {
                case RECEIVER:
                    rootBoolQuery.must(wildcardQuery("toEmail", userEmailLowerCase));
                    break;
                case RECEIVER_ANONYMOUS:
                    rootBoolQuery.must(wildcardQuery("toEmailAnonymous", userEmailLowerCase));
                    break;
                case SENDER:
                    rootBoolQuery.must(wildcardQuery("fromEmail", userEmailLowerCase));
                    break;
                case SENDER_ANONYMOUS:
                    rootBoolQuery.must(wildcardQuery("fromEmailAnonymous", userEmailLowerCase));
                    break;
                case ANY:
                    BoolQueryBuilder orQuery = boolQuery()
                            .should(wildcardQuery("fromEmail", userEmailLowerCase))
                            .should(wildcardQuery("toEmail", userEmailLowerCase))
                            .should(wildcardQuery("fromEmailAnonymous", userEmailLowerCase))
                            .should(wildcardQuery("toEmailAnonymous", userEmailLowerCase));

                    rootBoolQuery.must(orQuery);
                    break;
                default:
                    throw new IllegalStateException("Unknown enumeration value: " + payload.getUserRole().name());
            }
        }
    }

    private void setupMessageFreetextQuery() {
        if (!Strings.isNullOrEmpty(payload.getMessageTextKeywords())) {
            QueryStringQueryBuilder queryStringQuery = new QueryStringQueryBuilder(payload.getMessageTextKeywords()).field("messageText");

            if (!Strings.isNullOrEmpty(payload.getMessageTextMinimumShouldMatch())) {
                queryStringQuery = queryStringQuery.minimumShouldMatch(payload.getMessageTextMinimumShouldMatch());
            }

            rootBoolQuery.must(queryStringQuery);
        }
    }

    private void setupAdIdFilter() {
        if (!Strings.isNullOrEmpty(payload.getAdId())) {
            TermQueryBuilder query = QueryBuilders.termQuery("adId", payload.getAdId());
            rootBoolQuery.filter(query);
        }
    }

    private void setupMessageStateFilters() {
        if (payload.getMessageStates() != null) {
            String[] states = payload.getMessageStates().stream()
                    .map(MessageRtsState::name)
                    .toArray(String[]::new);

            TermsQueryBuilder query = QueryBuilders.termsQuery("messageState", states);
            rootBoolQuery.filter(query);
        }

        if (payload.getHumanResultState() != null) {
            TermQueryBuilder query = QueryBuilders.termQuery("humanResultState", payload.getHumanResultState().name());
            rootBoolQuery.filter(query);
        }
    }

    private void setupQueryForFilterRuleHitsFilter() {
        if (!Strings.isNullOrEmpty(payload.getFilterName())) {
            TermQueryBuilder query = QueryBuilders.termQuery("feedback.filterName", payload.getFilterName());
            rootBoolQuery.filter(query);
        }

        if (!Strings.isNullOrEmpty(payload.getFilterInstance())) {
            TermQueryBuilder query = QueryBuilders.termQuery("feedback.filterInstance", payload.getFilterInstance());
            rootBoolQuery.filter(query);
        }
    }

    private void setupPaging(SearchSourceBuilder request) {
        if (payload.getCount() > 0) {
            request.size(payload.getCount());
        }

        if (payload.getOffset() > 0) {
            request.from(payload.getOffset());
        }
    }

    private void setupTimeRangeFilter() {
        if (payload.getFromDate() != null || payload.getToDate() != null) {
            RangeQueryBuilder query = QueryBuilders.rangeQuery("receivedDate");

            if (payload.getFromDate() != null) {
                query.from(DATE_TIME_FORMATTER.print(payload.getFromDate().getTime()));
            }

            if (payload.getToDate() != null) {
                query.to(DATE_TIME_FORMATTER.print(payload.getToDate().getTime()));
            }

            rootBoolQuery.filter(query);
        }
    }
}
