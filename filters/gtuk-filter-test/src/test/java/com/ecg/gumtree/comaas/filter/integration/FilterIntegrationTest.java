package com.ecg.gumtree.comaas.filter.integration;

import com.ecg.gumtree.comaas.filter.knowngood.GumtreeKnownGoodFilterConfiguration;
import com.ecg.gumtree.comaas.filter.word.GumtreeWordFilterConfiguration;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static com.ecg.gumtree.comaas.filter.integration.Utils.readFileContent;
import static org.junit.Assert.*;

@Configuration
public class FilterIntegrationTest {
    private static final MailBuilder mailWithBadWordFromKnownGood;
    private static final MailBuilder mailWithNoBadWord;
    private static final MailBuilder mailWithBadWord;

    private static FilterObject wordFilterPoo;
    private static FilterObject wordFilterFoo;
    private static FilterObject knownGoodFilter;

    static {

        mailWithBadWordFromKnownGood = MailBuilder.aNewMail()
                .adId("1234")
                .from("foo@bar.com")
                .to("bar@foo.com")
                .customHeader("buyergood", "ok")
                .htmlBody("poo");

        mailWithBadWord = MailBuilder.aNewMail()
                .adId("1234")
                .from("foo@bar.com")
                .to("bar@foo.com")
                .htmlBody("poo");

        mailWithNoBadWord = MailBuilder.aNewMail()
                .adId("1234")
                .from("foo@bar.com")
                .to("bar@foo.com")
                .htmlBody("bar");

        try {
            Path wordFilterPathPoo = Paths.get(FilterIntegrationTest.class.getResource("/configs/word_filter_config_poo.json").toURI());
            ObjectNode wordFilterConfigPoo = (ObjectNode) JsonObjects.parse(readFileContent(wordFilterPathPoo));

            Path wordFilterPathFoo = Paths.get(FilterIntegrationTest.class.getResource("/configs/word_filter_config_foo.json").toURI());
            ObjectNode wordFilterConfigFoo = (ObjectNode) JsonObjects.parse(readFileContent(wordFilterPathFoo));

            Path knownGoodFilterPath = Paths.get(FilterIntegrationTest.class.getResource("/configs/knowngood_filter_config.json").toURI());
            ObjectNode knownGoodFilterConfig = (ObjectNode) JsonObjects.parse(readFileContent(knownGoodFilterPath));

            wordFilterPoo = new FilterObject(GumtreeWordFilterConfiguration.WordFilterFactory.class, wordFilterConfigPoo);
            wordFilterFoo = new FilterObject(GumtreeWordFilterConfiguration.WordFilterFactory.class, wordFilterConfigFoo);
            knownGoodFilter = new FilterObject(GumtreeKnownGoodFilterConfiguration.KnownGoodFilterFactory.class, knownGoodFilterConfig);
        } catch (Exception e) {
            fail("Could not create Path for filters configuration files");
        }
    }

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(
            new Properties() {{
                put("tenant", "gtuk");
            }},
            null, 20, false,
            new Class[]{FilterConfigurationIntegrationTest.class},
            "cassandra_schema.cql");

    /**
     * Loads the filters. They will be processed in the same order in which they are inside the array
     * (the first filter in the array will be processed first, and so on).
     */
    private void loadFilters(FilterObject... filters) {
        int priority = 200;
        for (FilterObject filterObject : filters) {
            rule.registerConfig(filterObject.filter, filterObject.config, priority);
            priority -= 10;
        }
    }

    private boolean isMessagePassed(MailBuilder mail) {
        return getProcessingFeedbacks(rule.deliver(mail)).size() == 0;
    }

    private List<ProcessingFeedback> getProcessingFeedbacks(MailInterceptor.ProcessedMail processedMail) {
        Message message = processedMail.getMessage();
        return message.getProcessingFeedback();
    }

    @Test
    public void shouldPassMessageIfKnownGoodIsBeforeFailingFilters() throws Exception {
        loadFilters(wordFilterFoo, knownGoodFilter, wordFilterPoo);
        assertTrue(isMessagePassed(mailWithBadWordFromKnownGood));
    }

    @Test
    public void shouldNotPassMessageIfKnownGoodIsAfterFailingFilters() {
        loadFilters(wordFilterFoo, wordFilterPoo, knownGoodFilter);
        assertFalse(isMessagePassed(mailWithBadWordFromKnownGood));
    }

    @Test
    public void shouldNotPassNotKnownGoodMessageIfKnownGoodFilterIsBeforeFailingFilters() {
        loadFilters(wordFilterFoo, knownGoodFilter, wordFilterPoo);
        assertFalse(isMessagePassed(mailWithBadWord));
    }

    @Test
    public void shouldPassMessageWithAnyFilterFailingRegardlessOfKnownGood() {
        loadFilters(wordFilterFoo, knownGoodFilter, wordFilterPoo);
        assertTrue(isMessagePassed(mailWithNoBadWord));
    }

    static class FilterObject {
        Class<? extends BasePluginFactory> filter;
        ObjectNode config;

        FilterObject(Class<? extends BasePluginFactory> filter, ObjectNode config) {
            this.filter = filter;
            this.config = config;
        }
    }
}
