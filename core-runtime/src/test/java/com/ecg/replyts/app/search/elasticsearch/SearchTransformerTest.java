package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.path.json.JsonPath.from;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SearchTransformerTest {
    @Mock
    Client client;

    String indexName = "indexName";

    @Test
    public void assertQueryFormForMessageState() throws Exception {
        SearchMessagePayload payload = new SearchMessagePayload();
        List<MessageRtsState> messageRtsStates = Arrays.asList(MessageRtsState.HELD, MessageRtsState.SENT, MessageRtsState.BLOCKED);
        payload.setMessageStates(messageRtsStates);

        SearchRequestBuilder builder = new SearchRequestBuilder(client);
        when(client.prepareSearch(indexName)).thenReturn(builder);

        SearchRequestBuilder searchRequestBuilder = SearchTransformer.translate(payload, client, indexName).intoQuery();

        String queryJson = searchRequestBuilder.toString();
        List<List<String>> queryMsgStates = from(queryJson).getList("query.filtered.filter.and.filters.terms.messageState");

        assertEquals(queryMsgStates.size(), 1);
        assertEquals(queryMsgStates.get(0).size(), 3);
        assertTrue(queryMsgStates.get(0).containsAll(Arrays.asList("HELD", "SENT", "BLOCKED")));
    }


}