package com.ecg.replyts.app.search;

import com.ecg.replyts.client.configclient.Configuration;
import com.ecg.replyts.client.configclient.Configuration.ConfigurationId;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.util.JsonObjects.Builder;
import com.ecg.replyts.core.api.webapi.commands.SearchMessageCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor.ProcessedMail;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.ecg.replyts.integration.test.filter.SubjectKeywordFilterFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static com.ecg.replyts.core.api.util.JsonObjects.builder;
import static com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ResultOrdering.NEWEST_FIRST;
import static com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ResultOrdering.OLDEST_FIRST;
import static com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule.ES_ENABLED;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author alindhorst (original author)
 */
public class SearchServiceAcceptanceTest {

    private static final String URL_TEMPLATE = "http://localhost:%d/screeningv2/" + SearchMessageCommand.MAPPING;
    private String url;

    private final String uuid = UUID.randomUUID().toString();

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(ES_ENABLED);

    @Before
    public void setup() {
        url = String.format(URL_TEMPLATE, rule.getHttpPort());
    }

    private ConfigurationId createFilter(Class<? extends FilterFactory> clazz, String instanceName) {
        ConfigurationId id = new ConfigurationId(clazz.getName(), instanceName);
        Configuration configuration = new Configuration(
                id, PluginState.ENABLED, 100L, JsonObjects.builder().attr("count", 500).attr("foo", "bar2").build());
        rule.getConfigClient().putConfiguration(configuration);
        return id;
    }

    private void deleteFilter(ConfigurationId configurationId) {
        rule.getConfigClient().deleteConfiguration(configurationId);
    }

    @Test
    public void checkSearchByAdId() throws Exception {
        //send a mail
        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody("checkSearchByAdId " + uuid).
                        adId(uuid)
        ));


        //Build JSON Request
        Builder builder = builder().attr("count", 500).attr("adId", uuid);
        //Send off request
        Response response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn();

        List<String> result = response.getBody().jsonPath().getList("body.conversation.adId");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(uuid));
    }

    @Test
    public void checkSearchByConversationCustomValue() throws Exception {
        //send a mail
        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").adId("23").
                        plainBody("checkSearchByConversationCustomValue " + uuid).
                        customHeader("l2-categoryid", "X")
        ));


        // Thread.sleep(1000);
        //Build JSON Request (custom header names are always lowercased)
        Builder builder = builder().attr("l2-categoryid", "X");
        Builder map = builder().attr("count", 500).attr("conversationCustomValues", builder);
        //Send off request
        System.out.println(map.toJson());
        Response response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(map.toJson()).post(url).
                andReturn();

        List<String> result = response.getBody().jsonPath().getList("body.conversation.conversationHeaders.customheader");

        assertThat(result.size(), is(1));

    }

    @Test
    public void checkSearchUsingCount() throws Exception {
        int count = 2;

        for (int i = 0; i <= count; i++) { //count + 1 mails
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchUsingCount " + uuid).adId(uuid)
                    ));

        }

        Builder builder = builder().attr("count", 500).attr("adId", uuid).attr("count", count);
        String response = RestAssured.expect().that().
                statusCode(HttpStatus.SC_OK).
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        JsonPath json = from(response);

        List<String> result = json.getList("body.id");
        assertThat(result.size(), is(count));
        int jsonCount = json.getInt("pagination.deliveredCount");
        assertThat(jsonCount, is(count));
    }

    @Test
    public void checkSearchByFilterInstance() throws Exception {
        //send a mail
        String instanceName = "subjectKeywordFilterFactory_" + uuid;
        ConfigurationId filterConfigId =
                createFilter(SubjectKeywordFilterFactory.class, instanceName);

        List<List<String>> result;
        try {
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchByFilterInstance " + uuid).
                                    adId(uuid).subject("DROPPED mail")
                    ));

            Builder builder = builder().attr("count", 500).attr("filterInstance", instanceName);
            String response = RestAssured.expect().that().statusCode(HttpStatus.SC_OK).
                    when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).asString();

            JsonPath json = from(response);
            result = json.getList("body.processingFeedback.filterInstance");
        } finally {
            deleteFilter(filterConfigId);
        }
        assertThat(result.size(), is(1)); //-> List of filterNames
        assertThat(result.get(0), hasItem(instanceName));
    }

    @Test
    public void checkSearchByFilterName() throws Exception {
        //send a mail
        UUID uuid = UUID.randomUUID();
        String instanceName = "subjectKeywordFilterFactory";
        String filterName = SubjectKeywordFilterFactory.class.getName();

        ConfigurationId filterConfigId = createFilter(SubjectKeywordFilterFactory.class, instanceName);

        List<List<String>> result;
        try {
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchByFilterName " + uuid).
                                    adId(uuid.toString()).subject("DROPPED mail")
                    ));

            Builder builder = builder().attr("count", 500).attr("filterName", filterName);
            String response = RestAssured.expect().that().statusCode(HttpStatus.SC_OK).
                    when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).asString();

            JsonPath json = from(response);
            result = json.getList("body.processingFeedback.filterName");
        } finally {
            deleteFilter(filterConfigId);
        }

        assertThat(result.size(), is(greaterThanOrEqualTo(1))); //-> List of filterNames
        for (List<String> feedback : result) {
            assertThat(feedback, hasItem(filterName));
        }
    }

    @Test
    public void checkSearchByFrom() throws Exception {
        //send a mail
        String fromEmail = uuid + "@from.com";

        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(fromEmail).to(uuid + "@to.com").
                        plainBody("checkSearchByFrom " + uuid).adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder().attr("count", 500).attr("userEmail", fromEmail).attr("userRole", SearchMessagePayload.ConcernedUserRole.SENDER.name());
        //Send off request
        String response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        List<String> direction = from(response).getList("body.messageDirection");
        assertThat(direction.size(), is(1));
        assertThat(MessageDirection.valueOf(direction.get(0)), is(MessageDirection.BUYER_TO_SELLER));
        List<String> result = from(response).getList("body.conversation.buyer");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(fromEmail));
    }

    @Test
    public void checkSearchByFromWithWildcards() throws Exception {
        //send a mail
        String fromEmail = uuid + "@from.com";

        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(fromEmail).to(uuid + "@to.com").
                        plainBody("checkSearchByFrom " + uuid).adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder().attr("count", 500).attr("userEmail", uuid + "@*.com").attr("userRole", SearchMessagePayload.ConcernedUserRole.SENDER.name());
        //Send off request
        String response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        List<String> result = from(response).getList("body.conversation.buyer");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(fromEmail));
    }

    @Test
    public void checkSearchByTo() throws Exception {
        //send a mail
        String toEmail = uuid + "@to.com";

        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(toEmail).
                        plainBody("checkSearchByFrom " + uuid).adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder().attr("count", 500).attr("userEmail", toEmail).attr("userRole", SearchMessagePayload.ConcernedUserRole.RECEIVER.name());
        //Send off request
        String response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        List<String> direction = from(response).getList("body.messageDirection");
        assertThat(direction.size(), is(1));
        assertThat(MessageDirection.valueOf(direction.get(0)), is(MessageDirection.BUYER_TO_SELLER));
        List<String> result = from(response).getList("body.conversation.seller");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(toEmail));
    }

    @Test
    public void checkSearchByToWithWildcard() throws Exception {
        //send a mail
        String toEmail = uuid + "@to.com";

        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(toEmail).
                        plainBody("checkSearchByFrom " + uuid).adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder().attr("count", 500).attr("userEmail", uuid + "@*.com").attr("userRole", SearchMessagePayload.ConcernedUserRole.RECEIVER.name());
        //Send off request
        String response = RestAssured.expect().
                statusCode(HttpStatus.SC_OK).and().
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        List<String> result = from(response).getList("body.conversation.seller");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(toEmail));
    }

    @Test
    public void checkSearchByHumanResultState() throws Exception {
        //send a mail
        ProcessedMail check = rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody("checkSearchByHumanResultState " + uuid).
                        adId(uuid).subject("Check")
        );
        rule.waitUntilIndexedInEs(check);


        Builder builder = builder().attr("count", 500).attr("humanResultState", ModerationResultState.UNCHECKED.name());
        String response = RestAssured.expect().that().statusCode(HttpStatus.SC_OK).
                when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).asString();

        JsonPath json = from(response);
        List<String> result = json.getList("body.humanResultState");

        assertThat(result.size(), is(greaterThanOrEqualTo(1))); //should be more than one if test isn't run in isolation
        for (String value : result) {
            assertThat(value, is(ModerationResultState.UNCHECKED.name()));
        }
    }


    @Test
    public void checkSearchByMessageState() throws Exception {
        //send a mail
        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody("checkSearchByMessageState " + uuid).
                        adId(uuid).subject("Check")
        ));


        Builder builder = builder().attr("count", 500).attr("messageState", MessageState.SENT.name());
        String response = RestAssured.expect().that().statusCode(HttpStatus.SC_OK).
                when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).asString();

        JsonPath json = from(response);
        List<String> result = json.getList("body.state");


        assertThat(result.size(), is(greaterThanOrEqualTo(1))); //should be more than one if test isn't run in isolation
        for (String value : result) {
            assertThat(value, is(MessageState.SENT.name()));
        }
    }

    @Test
    public void checkSearchByMessageTextKeyword() throws Exception {
        String text = "checkSearchByMessageTextKeyword " + uuid;
        String[] words = {"checkSearchByMessageTextKeyword", uuid};

        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody(text).
                        adId(uuid).subject("Check")
        ));

        Builder builder = builder().attr("count", 500).attr("messageTextKeywords", text);
        String response = RestAssured.expect().that().statusCode(HttpStatus.SC_OK).
                when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).asString();

        JsonPath json = from(response);
        String result = json.getString("body.text");
        for (String word : words) {
            assertThat(result.indexOf(word), greaterThanOrEqualTo(0));
        }
    }

    @Test
    public void checkSearchUsingOffset() throws Exception  {
        int count = 2;

        for (int i = 0; i < count; i++) {
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchUsingOffset " + uuid).adId(uuid)
                    ));
        }

        int offset = 1;
        Builder builder = builder().attr("count", 500).attr("adId", uuid).attr("offset", offset);
        String response = RestAssured.expect().that().
                statusCode(HttpStatus.SC_OK).
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        JsonPath json = from(response);

        List<String> result = json.getList("body.id");
        assertThat(result.size(), is(count - offset));
        int jsonCount = json.getInt("pagination.deliveredCount");
        int jsonOffset = json.getInt("pagination.from");
        int jsonTotal = json.getInt("pagination.totalCount");
        assertThat(jsonOffset, is(offset));
        assertThat(jsonCount, is(jsonTotal - jsonOffset));
    }

    @Test
    public void checkSearchUsingAscOrdering() throws Exception {
        int count = 2;

        for (int i = 0; i < count; i++) {
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + i + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchUsingAscOrdering " + uuid).adId(uuid)
                    ));

        }

        Builder builder = builder().attr("count", 500).attr("adId", uuid).attr("ordering", OLDEST_FIRST.
                name());
        String response = RestAssured.expect().that().
                statusCode(HttpStatus.SC_OK).
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        JsonPath json = from(response);

        List<String> result = json.getList("body.conversation.buyer");
        assertThat(result.size(), is(count));
        for (int i = 0; i < result.size(); i++) {
            String expectedFrom = uuid + i + "@from.com";
            assertThat(result.get(i), is(expectedFrom));
        }
    }

    @Test
    public void checkSearchUsingDescOrdering() throws Exception {
        int count = 2;

        for (int i = 0; i < count; i++) {
            rule.waitUntilIndexedInEs(rule.deliver(
                            MailBuilder.aNewMail().
                                    from(uuid + i + "@from.com").to(uuid + "@to.com").
                                    plainBody("checkSearchUsingDescOrdering " + uuid).adId(uuid)
                    ));

        }

        Builder builder = builder().attr("count", 500).attr("adId", uuid).attr("ordering", NEWEST_FIRST.
                name());
        String response = RestAssured.expect().that().
                statusCode(HttpStatus.SC_OK).
                when().request().contentType(
                ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        JsonPath json = from(response);

        List<String> result = json.getList("body.conversation.buyer");
        assertThat(result.size(), is(count));
        for (int i = 0; i < result.size(); i++) {
            String expectedFrom = uuid + (count - 1 - i) + "@from.com";
            assertThat(result.get(i), is(expectedFrom));
        }
    }

    @Test
    public void checkSearchUsingFromDate() throws Exception {
        //this one will be ignored
        rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody("1 - checkSearchUsingFromDate " + uuid).adId(uuid)
        );

        long start = System.currentTimeMillis();
        String match = "2 - checkSearchUsingFromDate " + uuid;
        //this one we'll check for
        rule.waitUntilIndexedInEs(rule.deliver(
                MailBuilder.aNewMail().
                        from(uuid + "@from.com").to(uuid + "@to.com").
                        plainBody(match).adId(uuid)
        ));

        Builder builder = builder().attr("count", 500).attr("fromDate", start).attr("adId", uuid);
        String response = RestAssured.expect().statusCode(HttpStatus.SC_OK).
                when().request().contentType(ContentType.JSON).body(builder.toJson()).post(url).
                andReturn().asString();

        List<String> hits = from(response).getList("body.text");
        assertThat(hits.size(), is(1));
        assertThat(hits.get(0), is(match));
    }

}
