package com.ecg.replyts.integration.elasticsearch;

import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener;
import com.ecg.replyts.integration.test.support.Waiter;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.concurrent.TimeUnit;

public class EsUtils {

    public static void waitUntilIndexed(AwaitMailSentProcessedListener.ProcessedMail item, Client searchClient) {
        String id = item.getConversation().getId() + "/" + item.getMessage().getId();
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch("replyts")
                .setTypes("message")
                .setQuery(QueryBuilders.termQuery("_id", id));

        Waiter.await(
                () -> searchClient.search(searchRequestBuilder.request()).actionGet().getHits().getTotalHits() > 0).
                within(10, TimeUnit.SECONDS);
    }
}