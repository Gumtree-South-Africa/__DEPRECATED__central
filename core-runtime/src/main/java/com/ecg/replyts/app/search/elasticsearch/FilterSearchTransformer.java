package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.springframework.util.StringUtils;

import static org.elasticsearch.index.query.FilterBuilders.boolFilter;

/**
 * Author: bpadhiar
 */
public final class FilterSearchTransformer extends SearchTransformer {
    FilterSearchTransformer(SearchMessagePayload payload, Client client, String indexName) {
        super(payload, client, indexName);
    }

    FilterSearchTransformer(SearchMessageGroupPayload payload, Client client, String indexName) {
        super(payload, client, indexName);
    }

    @Override
    protected void setupSearchSpecificParameters() {
        setupMessageFreetextFilter();
        setupMailAddressFilter();
        setupCustomValuesFilter();
    }

    private void setupMailAddressFilter() {
        if (StringUtils.hasText(payload.getUserEmail())) {
            switch (payload.getUserRole()) {
                case RECEIVER:
                    addFilter(new TermFilterBuilder("toEmail", payload.getUserEmail().toLowerCase()));
                    break;
                case SENDER:
                    addFilter(new TermFilterBuilder("fromEmail", payload.getUserEmail().toLowerCase()));
                    break;
                case ANY:
                    BoolFilterBuilder orFilter = boolFilter();
                    orFilter.should(new TermFilterBuilder("fromEmail", payload.getUserEmail().toLowerCase()));
                    orFilter.should(new TermFilterBuilder("toEmail", payload.getUserEmail().toLowerCase()));
                    addFilter(orFilter);
                    break;
                default:
                    throw new IllegalStateException("Unknown enumeration value: " + payload.getUserRole().name());
            }
        }
    }

    private void setupCustomValuesFilter() {
        if (!payload.getConversationCustomValues().isEmpty()) {
            BoolFilterBuilder andFilter = boolFilter();
            for (String key : payload.getConversationCustomValues().keySet()) {
                //by convention headers are always lowercased
                andFilter.must(new TermFilterBuilder("customHeaders." + key.toLowerCase(), payload.
                        getConversationCustomValues().get(key)));
            }
            addFilter(andFilter);
        }
    }

    private void setupMessageFreetextFilter() {
        if (StringUtils.hasText(payload.getMessageTextKeywords())) {

            addFilter(new TermFilterBuilder("messageText", payload.getMessageTextKeywords()));
        }
    }
}
