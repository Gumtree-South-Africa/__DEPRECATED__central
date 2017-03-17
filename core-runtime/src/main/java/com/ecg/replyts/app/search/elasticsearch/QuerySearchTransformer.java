package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.springframework.util.StringUtils;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

/**
 * Author: bpadhiar
 */
public final class QuerySearchTransformer extends SearchTransformer {
    QuerySearchTransformer(SearchMessagePayload payload, Client client, String indexName) {
        super(payload, client, indexName);
    }

    QuerySearchTransformer(SearchMessageGroupPayload payload, Client client, String indexName) {
        super(payload, client, indexName);
    }

    @Override
    protected void setupSearchSpecificParameters() {
        setupMessageFreetextQuery();
        setupMailAddressQuery();
        setupCustomValuesQuery();
    }

    private void setupMessageFreetextQuery() {
        if (StringUtils.hasText(payload.getMessageTextKeywords())) {

            addQuery(new QueryStringQueryBuilder(payload.getMessageTextKeywords()).field("messageText"));
        }
    }

    private void setupMailAddressQuery() {
        if (StringUtils.hasText(payload.getUserEmail())) {
            switch (payload.getUserRole()) {
                case RECEIVER:
                    addQuery(wildcardQuery("toEmail", payload.getUserEmail().toLowerCase()));
                    break;
                case SENDER:
                    addQuery(wildcardQuery("fromEmail", payload.getUserEmail().toLowerCase()));
                    break;
                case ANY:
                    BoolQueryBuilder orQuery = boolQuery();
                    orQuery.should(wildcardQuery("fromEmail", payload.getUserEmail().toLowerCase()));
                    orQuery.should(wildcardQuery("toEmail", payload.getUserEmail().toLowerCase()));
                    addQuery(orQuery);
                    break;
                default:
                    throw new IllegalStateException("Unknown enumeration value: " + payload.getUserRole().name());
            }
        }
    }

    private void setupCustomValuesQuery() {
        if (!payload.getConversationCustomValues().isEmpty()) {
            BoolQueryBuilder andQuery = boolQuery();
            for (String key : payload.getConversationCustomValues().keySet()) {
                //by convention headers are always lowercased
                andQuery.must(matchPhraseQuery("customHeaders." + key.toLowerCase(), payload.
                        getConversationCustomValues().get(key)));
            }
            addQuery(andQuery);
        }
    }
}