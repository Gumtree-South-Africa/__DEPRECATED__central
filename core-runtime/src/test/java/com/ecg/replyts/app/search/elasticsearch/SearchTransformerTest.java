package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchTransformerTest {
    private static final long FIXED_DATE = 1420070400000L; // Fixed date for testing purposes. 1st Jan 2015

    @Mock
    private Client elasticsearchClient;

    @Before
    public void setUp() throws Exception {
        SearchRequestBuilder builder = new SearchRequestBuilder(elasticsearchClient, SearchAction.INSTANCE);
        when(elasticsearchClient.prepareSearch("replyts")).thenReturn(builder);
    }

    @Test
    public void testSearchIsAFilteredQuery() throws Exception {
        // Binary searches (document either matches or does not, no ranking) should use a filtered filter. This does not
        // support wildcards, so we use a filtered query instead, for seemingly the same performance boost. This test
        // makes sure we're not using a non-filtered query, which is a severe performance penalty on large data sets.

        Date date = new Date(FIXED_DATE);
        SearchMessagePayload payload = new SearchMessagePayload();
        payload.setFromDate(date);
        payload.setUserEmail("sender@rts2.com");
        payload.setHumanResultState(ModerationResultState.GOOD);
        payload.setUserRole(SearchMessagePayload.ConcernedUserRole.SENDER);
        payload.setOrdering(SearchMessagePayload.ResultOrdering.NEWEST_FIRST);
        payload.setCount(1);

        SearchTransformer transformer = new SearchTransformer(payload, "replyts");
        SearchRequest searchRequestBuilder = transformer.intoQuery();

        JsonNode jsonNode = new ObjectMapper().readTree(searchRequestBuilder.toString());
        assertNotNull(jsonNode.get("query").get("bool").get("must"));

        // We want a search of the shape
        /*
            "query": {
                "bool": {
                    "must": {
                        ... etc
         */
    }
}