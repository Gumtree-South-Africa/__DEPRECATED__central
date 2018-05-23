package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.runtime.indexer.ElasticSearchIndexer;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ecg.replyts.core.runtime.logging.MDCConstants.setTaskFields;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
class IndexInvokeController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexInvokeController.class);

    @Autowired
    private ElasticSearchIndexer elasticSearchIndexer;

    private static final Splitter CONVERSATION_SPLITTER = Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(',')).or(CharMatcher.BREAKING_WHITESPACE)).trimResults().omitEmptyStrings();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("invokeIndexController-%s").build());

    @RequestMapping(value = "/startFullIndex", method = GET)
    @ResponseBody
    public String startFullIndex() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/reindex.html")) {
            return CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
        }
    }

    @RequestMapping("/indexConversations")
    @ResponseBody
    public String indexConversations(@RequestParam String conversationList) {
        LOG.debug("Conversations: {}", conversationList);
        final List<String> conversations = CONVERSATION_SPLITTER.splitToList(conversationList);
        LOG.info("Invoke index conversation via Web interface for conversations: " + conversations);
        executorService.execute(setTaskFields(() -> elasticSearchIndexer.indexConversations(conversations.stream()), "IndexInvokeController-indexChunk"));
        return "Index conversations by ID started.";
    }

    @RequestMapping(value = "/invokeReindex", method = POST)
    @ResponseBody
    public String invokeReindex(@RequestParam String user,
                                @RequestParam String reason,
                                @RequestParam boolean confirmed) {
        if (!confirmed || StringUtils.isBlank(user) || StringUtils.isBlank(reason)) {
            return "Please give a user & reason for full reindex and check the 'confirm' checkbox.";
        }

        LOG.info("Invoke Full Index via Web interface for user '{}' with reason '{}'", user, reason);
        executorService.execute(setTaskFields(elasticSearchIndexer::fullIndex, "IndexInvokeController-fullIndex"));

        return String.format("Full reindex started for user '%s' with reason '%s'.", user, reason);
    }

    @RequestMapping("/startIndexSince")
    @ResponseBody
    public String invokeIndexSince(@RequestParam("since") String since) {
        final DateTime sinceDate = DateTime.parse(since);
        LOG.info("Invoke Partial Full Index - since {}", sinceDate);
        executorService.execute(setTaskFields(() -> elasticSearchIndexer.indexSince(sinceDate), "IndexInvokeController-indexSince"));
        return "Rebuilding index since " + sinceDate;
    }

    @RequestMapping("/startIndexSinceTo")
    @ResponseBody
    public String invokeDeltaIndexTo(@RequestParam("since") String since,
                                     @RequestParam("to") String to) {
        LOG.info("Invoke Partial Full Index - since {} to - {}", since, to);
        final DateTime sinceDate = DateTime.parse(since);
        final DateTime toDate = DateTime.parse(to);
        executorService.execute(setTaskFields(() -> elasticSearchIndexer.doIndexBetween(sinceDate, toDate), "IndexInvokeController-indexSinceTo"));
        return "Rebuilding index since " + sinceDate + " to " + toDate;
    }

}
