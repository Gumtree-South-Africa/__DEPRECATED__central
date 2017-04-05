package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.api.indexer.Indexer;
import com.ecg.replyts.core.api.indexer.IndexerStatus;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.util.JsonObjects.Builder;
import com.ecg.replyts.core.runtime.indexer.IndexerChunkHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
class IndexInvokeController {

    private static final Logger LOG = LoggerFactory.getLogger(IndexInvokeController.class);

    @Autowired
    private Indexer indexer;

    @Autowired
    private IndexerChunkHandler chunkIndexer;

    private static final Splitter CONVERSATION_SPLITTER = Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(',')).or(CharMatcher.BREAKING_WHITESPACE)).trimResults().omitEmptyStrings();

    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("invokeIndexController-%s").build());

    @RequestMapping(value = "/startFullIndex", method = GET)
    @ResponseBody
    public String startFullIndex() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/reindex.html")) {
            return CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
        }
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
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                indexer.fullIndex();
            }
        });

        return String.format("Full reindex started for user '%s' with reason '%s'.", user, reason);
    }

    @RequestMapping("/indexConversations")
    @ResponseBody
    public String indexConversations(@RequestParam(required = true) String conversationList) {
        LOG.debug("Conversations: {}", conversationList);
        final List<String> conversations = CONVERSATION_SPLITTER.splitToList(conversationList);
        LOG.info("Invoke index conversation via Web interface for conversations: " + conversations);
        executorService.execute(() -> {
            chunkIndexer.indexChunk(conversations);
        });
        return "Index conversations by ID started.";
    }


    @RequestMapping("/startDeltaIndex")
    @ResponseBody
    public String invokeDeltaIndex() {
        LOG.info("Invoke Delta Index via Web interface");
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                indexer.deltaIndex();
            }
        });

        return "delta index started.";
    }

    @RequestMapping("/startIndexSince")
    @ResponseBody
    public String invokeDeltaIndex(String since) {
        final DateTime sinceDate = DateTime.parse(since);
        LOG.info("Invoke Partial Full Index - since {}", sinceDate);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                indexer.indexSince(sinceDate);
            }
        });
        return "Rebuilding index since " + sinceDate;
    }

    @RequestMapping(value = "/statusIndex", produces = "application/json")
    @ResponseBody
    public String statusIndex() {
        Builder response = JsonObjects.builder();
        response.attr("title", "Indexer Status");
        ArrayNode indexRuns = JsonObjects.newJsonArray();
        for (IndexerStatus status : indexer.getStatus()) {
            if (status != null) {
                double progress = ((double) status.getCompletedChunks()) / ((double) status.getTotalChunks());
                String dateFormat = "yyyy-MM-dd'T'hh:mm:ss";

                indexRuns.add(JsonObjects.builder()
                        .attr("mode", status.getMode())
                        .attr("index-from-date", status.getDateFrom().toString(dateFormat))
                        .attr("index-to-date", status.getDateFrom().toString(dateFormat))
                        .attr("chunks-total", status.getTotalChunks())
                        .attr("chunks-finished", status.getCompletedChunks())
                        .attr("process-progress", (progress * 100d) + "%")
                        .attr("process-estimated-runtime", estimateRuntime(status))
                        .attr("process-started", status.getStartDate().toString(dateFormat))
                        .attr("process-ended", status.getEndDate() == null ? null : status.getEndDate().toString(dateFormat))
                        .attr("process-running-on", status.getHostName())
                        .build());
            }
        }

        response.attr("runs", indexRuns);


        return response.toJson();
    }

    private Builder estimateRuntime(IndexerStatus status) {
        double progress = ((double) status.getCompletedChunks()) / ((double) status.getTotalChunks());
        double millis = (DateTime.now().getMillis() - status.getStartDate().getMillis()) / progress;
        return JsonObjects.builder()
                .attr("in-ms", millis)
                .attr("in-mins", millis / (1000d * 60d))
                .attr("in-hours", millis / (1000d * 60d * 60d))
                .attr("in-days", millis / (1000d * 60d * 60d * 24d));
    }
}
