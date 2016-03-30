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
import com.ecg.replyts.core.api.webapi.commands.SearchMessageGroupCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener.ProcessedMail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.ecg.replyts.integration.test.filter.SubjectKeywordFilterFactory;
import com.ecg.replyts.integration.test.support.Waiter;
import com.google.common.collect.ImmutableList;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.api.util.JsonObjects.builder;
import static com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ResultOrdering.NEWEST_FIRST;
import static com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload.ResultOrdering.OLDEST_FIRST;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class MessageGroupSearchServiceTest {

    private static final String URL_TEMPLATE = "http://localhost:%d/screeningv2/" + SearchMessageGroupCommand.MAPPING;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageGroupSearchServiceTest.class);
    private static final Long TIMEOUT_MS = 10000l;
    private String url;

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Before
    public void setup() {
        url = String.format(URL_TEMPLATE, rule.getHttpPort());
    }

    private ConfigurationId createFilter(Class<? extends FilterFactory> clazz, String instanceName) {
        ConfigurationId id = new ConfigurationId(clazz.getName(), instanceName);
        Configuration configuration = new Configuration(
                id, PluginState.ENABLED, 100l, JsonObjects.builder().attr("count", 25).attr("foo", "bar2").build());
        rule.getConfigClient().putConfiguration(configuration);
        return id;
    }

    private void deleteFilter(ConfigurationId configurationId) {
        rule.getConfigClient().deleteConfiguration(configurationId);
    }

    @Test
    public void groupByFromEmail_searchByAdId_singleEmail() throws IOException, InterruptedException {
        //send a mail
        UUID uuid = UUID.randomUUID();
        String from = uuid.toString() + "@from.com";
        ensureDocIndexed(rule.deliver(
                MailBuilder.aNewMail()
                        .from(from)
                        .to(uuid.toString() + "@to.com")
                        .plainBody("groupByFromEmail_searchByAdId_singleEmail " + uuid.toString())
                        .adId(uuid.toString())
                )
        );


        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid.toString());
        //Send off request
        Response response = RestAssured.expect()
                .statusCode(HttpStatus.SC_OK).and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn();

        List<List<String>> adIDs = response.getBody().jsonPath().getList("body.messages.conversation.adId");
        assertThat(adIDs.size(), is(1));
        assertThat(adIDs.get(0).size(), is(1));
        assertThat(adIDs.get(0).get(0), is(uuid.toString()));

        // Check group key (fromEmail)
        List<String> emailGroups = response.getBody().jsonPath().getList("body.key");
        assertThat(emailGroups.size(), is(1));
        assertThat(emailGroups.get(0), is(from));

        // Check pagination
        assertThat(response.getBody().jsonPath().getMap("pagination"), nullValue());
        Map<String, Integer> firstGroupPagination = response.getBody().jsonPath().getMap("body[0].pagination");
        assertThat(firstGroupPagination.get("from"), is(0));
        assertThat(firstGroupPagination.get("deliveredCount"), is(1));
        assertThat(firstGroupPagination.get("totalCount"), is(1));
    }

    @Test
    public void groupByFromEmail_searchByAdId_twoGroups() throws IOException, InterruptedException {
        // send three emails from one user and two emails from another user
        // the one with the most emails should be first in the results
        List<String> uuids = ImmutableList.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<String> froms = ImmutableList.of(uuids.get(0) + "@from.com", uuids.get(1) + "@from.com");
        String adId = uuids.get(0);

        // first user sent 3 replies
        for (int i = 0; i < 3; i++) {
            ensureDocIndexed(rule.deliver(
                            MailBuilder.aNewMail()
                                    .from(froms.get(0))
                                    .to(adId + "@to.com")
                                    .plainBody("groupByFromEmail_searchByAdId_twoGroups " + adId)
                                    .adId(adId)
                    )
            );
        }

        // second user sent only 2 replies
        for (int i = 0; i < 2; i++) {
            ensureDocIndexed(rule.deliver(
                            MailBuilder.aNewMail()
                                    .from(froms.get(1))
                                    .to(adId + "@to.com")
                                    .plainBody("checkSearchByAdId " + adId)
                                    .adId(adId)
                    )
            );
        }

        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", adId);
        //Send off request
        Response response = RestAssured.expect()
                .statusCode(HttpStatus.SC_OK).and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn();

        // Check group key (fromEmail)
        List<String> emailGroups = response.getBody().jsonPath().getList("body.key");
        assertThat(emailGroups.size(), is(2));
        assertThat(emailGroups.get(0), is(froms.get(0)));
        assertThat(emailGroups.get(1), is(froms.get(1)));

        List<List<String>> adIDs = response.getBody().jsonPath().getList("body.messages.conversation.adId");
        assertThat(adIDs.get(0).size(), is(3));
        assertThat(adIDs.get(1).size(), is(2));
        assertThat(adIDs.get(0).get(0), is(adId));
        assertThat(adIDs.get(0).get(1), is(adId));
        assertThat(adIDs.get(0).get(2), is(adId));
        assertThat(adIDs.get(1).get(0), is(adId));
        assertThat(adIDs.get(1).get(1), is(adId));

        // Check pagination
        assertThat(response.getBody().jsonPath().getMap("pagination"), nullValue());
        Map<String, Integer> firstGroupPagination = response.getBody().jsonPath().getMap("body[0].pagination");
        assertThat(firstGroupPagination.get("from"), is(0));
        assertThat(firstGroupPagination.get("deliveredCount"), is(3));
        assertThat(firstGroupPagination.get("totalCount"), is(3));
        Map<String, Integer> secondGroupPagination = response.getBody().jsonPath().getMap("body[1].pagination");
        assertThat(secondGroupPagination.get("from"), is(0));
        assertThat(secondGroupPagination.get("deliveredCount"), is(2));
        assertThat(secondGroupPagination.get("totalCount"), is(2));
    }

    @Test
    public void groupByFromMail_11emailsSent_10emailsInGroup() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String from = uuid + "@from.com";

        for (int i = 0; i < 11; i++) {
            ensureDocIndexed(rule.deliver(
                            MailBuilder.aNewMail()
                                    .from(from)
                                    .to(uuid + "@to.com")
                                    .plainBody("groupByFromMail_11emailsSent_10emailsInGroup " + uuid)
                                    .adId(uuid)
                    )
            );
        }

        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid);
        //Send off request
        Response response = RestAssured.expect()
                .statusCode(HttpStatus.SC_OK).and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn();

        // Check group key (fromEmail)
        List<String> emailGroups = response.getBody().jsonPath().getList("body.key");
        assertThat(emailGroups.size(), is(1));
        assertThat(emailGroups.get(0), is(from));

        List<List<String>> adIDs = response.getBody().jsonPath().getList("body.messages.conversation.adId");
        assertThat(adIDs.get(0).size(), is(10));
        for (String adID : adIDs.get(0)) {
            assertThat(adID, is(uuid));
        }

        // Check pagination
        assertThat(response.getBody().jsonPath().getMap("pagination"), nullValue());
        Map<String, Integer> firstGroupPagination = response.getBody().jsonPath().getMap("body[0].pagination");
        assertThat(firstGroupPagination.get("from"), is(0));
        assertThat(firstGroupPagination.get("deliveredCount"), is(10));
        assertThat(firstGroupPagination.get("totalCount"), is(11));
    }

    @Test
    public void groupByFromMail_checkSearchByFilterInstance() throws Exception {
        List<List<List<String>>> result;
        //send a mail
        String uuid = UUID.randomUUID().toString();
        String instanceName = "subjectKeywordFilterFactory_" + uuid;
        final ConfigurationId filterConfigId =
                createFilter(SubjectKeywordFilterFactory.class, instanceName);
        waitForFilterConfigToBeAdded(filterConfigId);


        try {
            ensureDocIndexed(rule.deliver(
                            MailBuilder
                                    .aNewMail()
                                    .from(uuid + "@from.com")
                                    .to(uuid + "@to.com")
                                    .plainBody("groupByFromMail_checkSearchByFilterInstance " + uuid)
                                    .adId(uuid)
                                    .subject("DROPPED mail")
                    )
            );

            Builder builder = builder()
                    .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                    .attr("count", 25)
                    .attr("filterInstance", instanceName);
            String response = RestAssured
                    .expect()
                    .that()
                    .statusCode(HttpStatus.SC_OK)
                    .when()
                    .request()
                    .contentType(ContentType.JSON)
                    .body(builder.toJson())
                    .post(url)
                    .asString();

            LOGGER.debug(response);
            JsonPath json = from(response);
            result = json.getList("body.messages.processingFeedback.filterInstance");
            assertThat(result.size(), is(1)); // one group
            assertThat(result.get(0).size(), is(1)); // one message in group
            assertThat(result.get(0).get(0).size(), is(2)); // two filter instances for message (one configured, one for end of filter chain)
            assertThat(result.get(0).get(0), hasItem(instanceName));
        } finally {
            deleteFilter(filterConfigId);
            waitForFilterConfigToBeDeleted(filterConfigId);
        }
    }

    @Test
    public void groupByFromMail_checkSearchByFilterName() throws Exception {
        List<List<List<String>>> result;
        //send a mail
        String uuid = UUID.randomUUID().toString();
        String instanceName = "subjectKeywordFilterFactory";
        String filterName = SubjectKeywordFilterFactory.class.getName();

        ConfigurationId filterConfigId = createFilter(SubjectKeywordFilterFactory.class, instanceName);
        waitForFilterConfigToBeAdded(filterConfigId);

        try {
            ensureDocIndexed(rule.deliver(
                    MailBuilder
                            .aNewMail()
                            .from(uuid + "@from.com")
                            .to(uuid + "@to.com")
                            .plainBody("groupByFromMail_checkSearchByFilterName " + uuid)
                            .adId(uuid)
                            .subject("DROPPED mail")
            ));

            Builder builder = builder()
                    .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                    .attr("count", 25)
                    .attr("adId", uuid)
                    .attr("filterName", filterName);
            String response = RestAssured
                    .expect()
                    .that()
                    .statusCode(HttpStatus.SC_OK)
                    .when()
                    .request()
                    .contentType(ContentType.JSON)
                    .body(builder.toJson())
                    .post(url)
                    .asString();

            LOGGER.debug(response);
            JsonPath json = from(response);
            result = json.getList("body.messages.processingFeedback.filterName");
            assertThat(result.size(), is(1)); // one group
            assertThat(result.get(0).size(), is(1)); // one message
            for (List<String> feedback : result.get(0)) {
                assertThat(feedback, hasItem(filterName));
            }
        } finally {
            deleteFilter(filterConfigId);
            waitForFilterConfigToBeDeleted(filterConfigId);
        }
    }

    @Test
    public void groupByFromMail_checkSearchByFrom() {
        //send a mail
        String uuid = UUID.randomUUID().toString();
        String fromEmail = uuid + "@from.com";

        ensureDocIndexed(rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(fromEmail)
                        .to(uuid + "@to.com")
                        .plainBody("groupByFromMail_checkSearchByFrom " + uuid)
                        .adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("userEmail", fromEmail)
                .attr("userRole", SearchMessagePayload.ConcernedUserRole.SENDER.name());
        //Send off request
        String response = RestAssured
                .expect()
                .statusCode(HttpStatus.SC_OK)
                .and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        LOGGER.debug(response);

        List<List<String>> direction = from(response).getList("body.messages.messageDirection");
        assertThat(direction.size(), is(1)); // one group
        assertThat(direction.get(0).size(), is(1)); // one message
        assertThat(MessageDirection.valueOf(direction.get(0).get(0)), is(MessageDirection.BUYER_TO_SELLER));
        List<List<String>> result = from(response).getList("body.messages.conversation.buyer");
        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(1)); // one message
        assertThat(result.get(0).get(0), is(fromEmail));
    }

    @Test
    public void groupByFromMail_checkSearchByTo() {
        //send a mail
        String uuid = UUID.randomUUID().toString();
        String toEmail = uuid + "@to.com";

        ensureDocIndexed(rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(toEmail)
                        .plainBody("groupByFromMail_checkSearchByTo " + uuid)
                        .adId(uuid)
        ));

        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("userEmail", toEmail)
                .attr("userRole", SearchMessagePayload.ConcernedUserRole.RECEIVER.name());
        //Send off request
        String response = RestAssured
                .expect()
                .statusCode(HttpStatus.SC_OK)
                .and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        LOGGER.debug(response);

        List<List<String>> direction = from(response).getList("body.messages.messageDirection");
        assertThat(direction.size(), is(1)); // one group
        assertThat(direction.get(0).size(), is(1)); // one message
        assertThat(MessageDirection.valueOf(direction.get(0).get(0)), is(MessageDirection.BUYER_TO_SELLER));
        List<List<String>> result = from(response).getList("body.messages.conversation.seller");
        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(1)); // one message
        assertThat(result.get(0).get(0), is(toEmail));
    }

    @Test
    public void groupByFromMail_checkSearchByHumanResultState() {
        //send a mail
        String uuid = UUID.randomUUID().toString();

        ProcessedMail check = rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(uuid + "@to.com")
                        .plainBody("groupByFromMail_checkSearchByHumanResultState " + uuid)
                        .adId(uuid)
                        .subject("Check")
        );
        ensureDocIndexed(check);


        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid)
                .attr("humanResultState", ModerationResultState.UNCHECKED.name());
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .asString();

        LOGGER.debug(response);
        JsonPath json = from(response);
        List<List<String>> result = json.getList("body.messages.humanResultState");

        assertThat(result.size(), is(1)); //should be more than one if test isn't run in isolation
        assertThat(result.get(0).size(), is(1)); //should be more than one if test isn't run in isolation
        for (String value : result.get(0)) {
            assertThat(value, is(ModerationResultState.UNCHECKED.name()));
        }
    }



    @Test
    public void groupByFromMail_checkSearchByMessageState() {
        //send a mail
        String uuid = UUID.randomUUID().toString();
        ensureDocIndexed(rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(uuid + "@to.com")
                        .plainBody("groupByFromMail_checkSearchByMessageState " + uuid)
                        .adId(uuid)
                        .subject("Check")
        ));


        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid)
                .attr("messageState", MessageState.SENT.name());
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .asString();

        LOGGER.debug(response);
        JsonPath json = from(response);
        List<List<String>> result = json.getList("body.messages.state");

        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(1)); // one message
        assertThat(result.get(0).get(0), is(MessageState.SENT.name()));
    }

    @Test
    public void groupByFromMail_checkSearchByMessageTextKeyword() {
        String uuid = UUID.randomUUID().toString();
        String text = "groupByFromMail_checkSearchByMessageTextKeyword " + uuid;
        String[] words = {"groupByFromMail_checkSearchByMessageTextKeyword", uuid};

        ensureDocIndexed(rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(uuid + "@to.com")
                        .plainBody(text)
                        .adId(uuid)
                        .subject("Check")
        ));

        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("messageTextKeywords", text);
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .asString();

        LOGGER.debug(response);
        JsonPath json = from(response);
        assertThat(json.getList("body").size(), is(1));
        assertThat(json.getList("body.messages").size(), is(1));
        String result = json.getString("body[0].messages[0].text");
        for (String word : words) {
            assertThat(result.indexOf(word), greaterThanOrEqualTo(0));
        }
    }

    @Test
    public void groupByFromMail_checkOffsetOnlyUsedWithinGroups() {
        String uuid = UUID.randomUUID().toString();

        for (int i = 0; i < 2; i++) {
            ensureDocIndexed(rule.deliver(
                    MailBuilder
                            .aNewMail()
                            .from(uuid + "@from.com")
                            .to(uuid + "@to.com")
                            .plainBody("checkSearchUsingOffset " + uuid)
                            .adId(uuid)
            ));
        }

        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid)
                .attr("offset", 1);
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        JsonPath json = from(response);

        List<List<String>> result = json.getList("body.messages.id");
        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(1)); // one message
        assertThat(json.getString("pagination"), nullValue());
        int jsonCount = json.getInt("body[0].pagination.deliveredCount");
        int jsonOffset = json.getInt("body[0].pagination.from");
        int jsonTotal = json.getInt("body[0].pagination.totalCount");
        assertThat(jsonOffset, is(1));
        assertThat(jsonCount, is(1));
        assertThat(jsonTotal, is(2));
    }

    @Test
    public void groupByFromMail_checkSearchUsingAscOrdering() {
        int count = 2;
        String uuid = UUID.randomUUID().toString();

        for (int i = 0; i < count; i++) {
            ensureDocIndexed(rule.deliver(
                    MailBuilder
                            .aNewMail()
                            .from(uuid + "@from.com")
                            .to(uuid + i + "@to.com")
                            .plainBody("checkSearchUsingAscOrdering " + uuid)
                            .adId(uuid)
            ));

        }

        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid)
                .attr("ordering", OLDEST_FIRST.name());
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        JsonPath json = from(response);

        List<List<String>> result = json.getList("body.messages.conversation.seller");
        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(2)); // two msgs
        for (int i = 0; i < count; i++) {
            String expectedTo = uuid + i + "@to.com";
            assertThat(result.get(0).get(i), is(expectedTo));
        }
    }

    @Test
    public void groupByFromMail_checkSearchUsingDescOrdering() {
        String uuid = UUID.randomUUID().toString();
        int count = 2;

        for (int i = 0; i < count; i++) {
            ensureDocIndexed(rule.deliver(
                    MailBuilder
                            .aNewMail()
                            .from(uuid + "@from.com")
                            .to(uuid + i + "@to.com")
                            .plainBody("checkSearchUsingDescOrdering " + uuid)
                            .adId(uuid)
            ));

        }

        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid)
                .attr("ordering", NEWEST_FIRST.name());
        String response = RestAssured
                .expect()
                .that()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        JsonPath json = from(response);

        List<List<String>> result = json.getList("body.messages.conversation.seller");
        assertThat(result.size(), is(1)); // one group
        assertThat(result.get(0).size(), is(count)); // two messages
        for (int i = 0; i < count; i++) {
            String expectedTo = uuid + (count - 1 - i) + "@to.com";
            assertThat(result.get(0).get(i), is(expectedTo));
        }
    }

    @Test
    public void groupByFromMail_checkSearchUsingFromDate() {
        String uuid = UUID.randomUUID().toString();

        //this one will be ignored
        rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(uuid + "@to.com")
                        .plainBody("1 - checkSearchUsingFromDate " + uuid)
                        .adId(uuid)
        );

        long start = System.currentTimeMillis();
        String secondMessageContents = "2 - checkSearchUsingFromDate " + uuid;
        //this one we'll check for
        ensureDocIndexed(rule.deliver(
                MailBuilder
                        .aNewMail()
                        .from(uuid + "@from.com")
                        .to(uuid + "@to.com")
                        .plainBody(secondMessageContents)
                        .adId(uuid)
        ));

        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.FROM_EMAIL_ES_FIELDNAME)
                .attr("count", 25)
                .attr("fromDate", start)
                .attr("adId", uuid);
        String response = RestAssured
                .expect()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn()
                .asString();

        List<List<String>> hits = from(response).getList("body.messages.text");
        assertThat(hits.size(), is(1)); // one group
        assertThat(hits.get(0).size(), is(1)); // one message
        assertThat(hits.get(0).get(0), is(secondMessageContents)); // second message only
    }

    @Test
    public void groupByIP_searchByAdId_singleEmail() throws IOException, InterruptedException {
        //send a mail
        UUID uuid = UUID.randomUUID();
        String ip = "10.10.10.10";
        ensureDocIndexed(rule.deliver(
                        MailBuilder.aNewMail()
                                .from(uuid.toString() + "@from.com")
                                .to(uuid.toString() + "@to.com")
                                .plainBody("groupByIP_searchByAdId_singleEmail " + uuid.toString())
                                .adId(uuid.toString())
                                .customHeader("Ip-Address", ip)
                )
        );


        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.IP_ADDRESS_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", uuid.toString());
        //Send off request
        Response response = RestAssured.expect()
                .statusCode(HttpStatus.SC_OK).and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn();

        List<List<String>> adIDs = response.getBody().jsonPath().getList("body.messages.conversation.adId");
        assertThat(adIDs.size(), is(1));
        assertThat(adIDs.get(0).size(), is(1));
        assertThat(adIDs.get(0).get(0), is(uuid.toString()));

        // Check group key (ip)
        List<String> ipGroups = response.getBody().jsonPath().getList("body.key");
        assertThat(ipGroups.size(), is(1));
        assertThat(ipGroups.get(0), is(ip));

        // Check pagination
        assertThat(response.getBody().jsonPath().getMap("pagination"), nullValue());
        Map<String, Integer> firstGroupPagination = response.getBody().jsonPath().getMap("body[0].pagination");
        assertThat(firstGroupPagination.get("from"), is(0));
        assertThat(firstGroupPagination.get("deliveredCount"), is(1));
        assertThat(firstGroupPagination.get("totalCount"), is(1));
    }

    @Test
    public void groupByIP_searchByAdId_twoGroups() throws IOException, InterruptedException {
        // send three emails from one user and two emails from another user
        // the one with the most emails should be first in the results
        List<String> uuids = ImmutableList.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<String> ips = ImmutableList.of("10.10.10.10", "12.12.12.12");
        String adId = uuids.get(0);

        // first user sent 3 replies
        for (int i = 0; i < 3; i++) {
            ensureDocIndexed(rule.deliver(
                            MailBuilder.aNewMail()
                                    .from(uuids.get(0) + "@from.com")
                                    .to(adId + "@to.com")
                                    .plainBody("groupByFromEmail_searchByAdId_twoGroups " + adId)
                                    .adId(adId)
                                    .customHeader("Ip-Address", ips.get(0))
                    )
            );
        }

        // second user sent only 2 replies
        for (int i = 0; i < 2; i++) {
            ensureDocIndexed(rule.deliver(
                            MailBuilder.aNewMail()
                                    .from(uuids.get(1) + "@from.com")
                                    .to(adId + "@to.com")
                                    .plainBody("checkSearchByAdId " + adId)
                                    .adId(adId)
                                    .customHeader("Ip-Address", ips.get(1))
                    )
            );
        }

        //Build JSON Request
        Builder builder = builder()
                .attr("groupBy", SearchMessageGroupPayload.IP_ADDRESS_ES_FIELDNAME)
                .attr("count", 25)
                .attr("adId", adId);
        //Send off request
        Response response = RestAssured.expect()
                .statusCode(HttpStatus.SC_OK).and()
                .when()
                .request()
                .contentType(ContentType.JSON)
                .body(builder.toJson())
                .post(url)
                .andReturn();

        // Check group key (ip)
        List<String> emailGroups = response.getBody().jsonPath().getList("body.key");
        assertThat(emailGroups.size(), is(2));
        assertThat(emailGroups.get(0), is(ips.get(0)));
        assertThat(emailGroups.get(1), is(ips.get(1)));

        List<List<String>> adIDs = response.getBody().jsonPath().getList("body.messages.conversation.adId");
        assertThat(adIDs.get(0).size(), is(3));
        assertThat(adIDs.get(1).size(), is(2));
        assertThat(adIDs.get(0).get(0), is(adId));
        assertThat(adIDs.get(0).get(1), is(adId));
        assertThat(adIDs.get(0).get(2), is(adId));
        assertThat(adIDs.get(1).get(0), is(adId));
        assertThat(adIDs.get(1).get(1), is(adId));

        // Check pagination
        assertThat(response.getBody().jsonPath().getMap("pagination"), nullValue());
        Map<String, Integer> firstGroupPagination = response.getBody().jsonPath().getMap("body[0].pagination");
        assertThat(firstGroupPagination.get("from"), is(0));
        assertThat(firstGroupPagination.get("deliveredCount"), is(3));
        assertThat(firstGroupPagination.get("totalCount"), is(3));
        Map<String, Integer> secondGroupPagination = response.getBody().jsonPath().getMap("body[1].pagination");
        assertThat(secondGroupPagination.get("from"), is(0));
        assertThat(secondGroupPagination.get("deliveredCount"), is(2));
        assertThat(secondGroupPagination.get("totalCount"), is(2));
    }


    private void ensureDocIndexed(ProcessedMail item) {
        String id = item.getConversation().getId() + "/" + item.getMessage().getId();

        long end = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < end) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            SearchRequestBuilder searchRequestBuilder = rule.getSearchClient().prepareSearch("replyts")
                    .setTypes("message")
                    .setQuery(QueryBuilders.termQuery("_id", id));

            boolean exists = rule.getSearchClient().search(searchRequestBuilder.request()).actionGet().getHits().getTotalHits() > 0;

            if (exists) {
                return;
            }
        }

        throw new IllegalStateException("mail was not indexed :(");

    }

    private void waitForFilterConfigToBeAdded(final ConfigurationId filterConfigId) {
        Waiter.await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<Configuration> configurations = rule.getConfigClient().listConfigurations();
                for (Configuration configuration : configurations) {
                    if (configuration.getConfigurationId().getInstanceId().equals(filterConfigId.getInstanceId())) {
                        return true;
                    }
                }
                return false;
            }
        }).within(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void waitForFilterConfigToBeDeleted(final ConfigurationId filterConfigId) {
        Waiter.await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                List<Configuration> configurations = rule.getConfigClient().listConfigurations();
                for (Configuration configuration : configurations) {
                    if (configuration.getConfigurationId().getInstanceId().equals(filterConfigId.getInstanceId())) {
                        return false;
                    }
                }
                return true;
            }
        }).within(TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

}
