package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Author: bpadhiar
 */
public class SearchTransformerTest {
    private static final long FIXED_DATE = 1420070400000L; // Fixed date for testing purposes. 1st Jan 2015

    private SearchRequestBuilder builder;
    private Client elasticsearchClient;

    @Before
    public void setUp() throws Exception {
        elasticsearchClient = mock(Client.class);
        builder = new SearchRequestBuilder(elasticsearchClient);
        when(elasticsearchClient.prepareSearch("replyts")).thenReturn(builder);
    }

    @Test
    public void testFilterQueryWithEmailForVelocity() throws Exception {
        Date date = new Date(FIXED_DATE);
        SearchMessagePayload payload = new SearchMessagePayload();
        payload.setFromDate(date);
        payload.setUserEmail("sender@rts2.com");
        payload.setHumanResultState(ModerationResultState.GOOD);
        payload.setUserRole(SearchMessagePayload.ConcernedUserRole.SENDER);
        payload.setOrdering(SearchMessagePayload.ResultOrdering.NEWEST_FIRST);
        payload.setCount(1);
        payload.setUseFilterQuery(true);

        SearchTransformer transformer = new FilterSearchTransformer(payload, elasticsearchClient, "replyts");
        SearchRequestBuilder searchRequestBuilder = transformer.intoQuery();
        JSONAssert.assertEquals(searchRequestBuilder.toString(), "{\n" +
                "  \"size\" : 1,\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"filter\" : {\n" +
                "        \"and\" : {\n" +
                "          \"filters\" : [ {\n" +
                "            \"range\" : {\n" +
                "              \"receivedDate\" : {\n" +
                "                \"to\" : null,\n" +
                "                \"from\" : \"2015-01-01T00:00:00.000Z\",\n" +
                "                \"include_lower\" : true,\n" +
                "                \"include_upper\" : true\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"term\" : {\n" +
                "              \"humanResultState\" : \"GOOD\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"term\" : {\n" +
                "              \"fromEmail\" : \"sender@rts2.com\"\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"receivedDate\" : {\n" +
                "      \"order\" : \"desc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}", false);

    }

    @Test
    public void testFilterQueryWithCustomHeaderForVelocity() throws Exception {

        Date date = new Date(FIXED_DATE);
        SearchMessagePayload payload = new SearchMessagePayload();
        payload.setConversationCustomValues(ImmutableMap.of("cookieID", "test-rts-cookie"));
        payload.setFromDate(date);
        payload.setHumanResultState(ModerationResultState.GOOD);
        payload.setUserRole(SearchMessagePayload.ConcernedUserRole.SENDER);
        payload.setOrdering(SearchMessagePayload.ResultOrdering.NEWEST_FIRST);
        payload.setCount(1);
        payload.setUseFilterQuery(true);

        SearchTransformer transformer = new FilterSearchTransformer(payload, elasticsearchClient, "replyts");
        SearchRequestBuilder searchRequestBuilder = transformer.intoQuery();
        JSONAssert.assertEquals(searchRequestBuilder.toString(), "{\n" +
                "  \"size\" : 1,\n" +
                "  \"query\" : {\n" +
                "    \"filtered\" : {\n" +
                "      \"filter\" : {\n" +
                "        \"and\" : {\n" +
                "          \"filters\" : [ {\n" +
                "            \"range\" : {\n" +
                "              \"receivedDate\" : {\n" +
                "                \"from\" : \"2015-01-01T00:00:00.000Z\",\n" +
                "                \"to\" : null,\n" +
                "                \"include_lower\" : true,\n" +
                "                \"include_upper\" : true\n" +
                "              }\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"term\" : {\n" +
                "              \"humanResultState\" : \"GOOD\"\n" +
                "            }\n" +
                "          }, {\n" +
                "            \"bool\" : {\n" +
                "              \"must\" : {\n" +
                "                \"term\" : {\n" +
                "                  \"customHeaders.cookieid\" : \"test-rts-cookie\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          } ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"sort\" : [ {\n" +
                "    \"receivedDate\" : {\n" +
                "      \"order\" : \"desc\"\n" +
                "    }\n" +
                "  } ]\n" +
                "}", false);
    }
}