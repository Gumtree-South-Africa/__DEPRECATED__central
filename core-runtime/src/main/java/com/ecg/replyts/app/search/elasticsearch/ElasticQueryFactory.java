package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.index.reindex.DeleteByQueryRequestBuilder;

import java.time.LocalDate;

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

class ElasticQueryFactory {

    private final Client client;
    private final String indexName;

    ElasticQueryFactory(Client client, String indexName) {
        this.client = client;
        this.indexName = indexName;
    }

    SearchRequestBuilder searchMessage(SearchMessagePayload payload) {
        return new SearchTransformer(payload, client, indexName).intoQuery();
    }

    DeleteByQueryRequestBuilder deleteLastModified(LocalDate from, LocalDate to) {
        RangeQueryBuilder lastModified = rangeQuery("lastModified")
                .from(from)
                .to(to);

        return DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .source(indexName)
                .filter(lastModified);
    }

    DeleteByQueryRequestBuilder deleteReceivedDate(LocalDate from, LocalDate to) {
        RangeQueryBuilder receivedDateQuery = rangeQuery("receivedDate")
                .from(from)
                .to(to);

        BoolQueryBuilder lastModified = QueryBuilders.boolQuery()
                .filter(receivedDateQuery)
                .mustNot(QueryBuilders.existsQuery("lastModified"));

        return DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .source(indexName)
                .filter(lastModified);
    }
}
