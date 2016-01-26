package com.ecg.replyts.migrations.cleanupoptimizer;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.DateSliceIterator;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
@Controller
class CleanupOptimizerController {

    private static final Logger LOG = LoggerFactory.getLogger("migrations");

    private static final int PROGRESS_CHUNK = 501;

    private final ConversationRepository convRepository;
    private final ConversationMigrator migrator;

    @Autowired
    CleanupOptimizerController(ConversationRepository convRepository, ConversationMigrator migrator) {
        this.convRepository = convRepository;
        this.migrator = migrator;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(new DateTime(text));
            }
        });

    }

    @RequestMapping(value = "/cleanup-optimizer", produces = "application/json")
    @ResponseBody
    public Object startMigration(
            @RequestParam("from") DateTime from,
            @RequestParam("to") DateTime to,
            @RequestParam("interval") Integer interval,
            @RequestParam("timeUnit") TimeUnit timeUnit) {

        DateSliceIterator slicer = new DateSliceIterator(Range.closed(from, to), interval, timeUnit, DateSliceIterator.IterationDirection.PAST_TO_PRESENT);

        int counter = 0;

        for (Range<DateTime> dateTimeRange : slicer) {
            List<String> convIds = convRepository.listConversationsCreatedBetween(dateTimeRange.lowerEndpoint(), dateTimeRange.upperEndpoint());
            LOG.info("Start migration for {} on convIds size {}", dateTimeRange, convIds.size());
            Stopwatch stopwatch = Stopwatch.createStarted();
            for (String convId : convIds) {
                migrate(convId);
                logProgress(counter, stopwatch);
                counter++;
            }
            LOG.info("Finished migration convIds size {} for time-range {}", convIds.size(), dateTimeRange);

        }

        return JsonObjects.builder().attr("operation", "cleanup-optimizer").attr("from", from.toString()).attr("to", to.toString()).toJson();
    }

    @RequestMapping(value = "/cleanup-optimizer/{convId}", produces = "application/json")
    @ResponseBody
    public void startMigration(@PathVariable("convId") String convId) {
        migrate(convId);
    }

    private void migrate(String convId) {
        migrator.migrate(convId);
    }


    private void logProgress(int counter, Stopwatch stopwatch) {
        if (counter % PROGRESS_CHUNK == PROGRESS_CHUNK - 1) {
            long elapsedTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            double througput = (PROGRESS_CHUNK / (double) elapsedTime) * 1000;
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            LOG.info("Progress: Finished overall {} , Throughput: {} conv/sec", counter, elapsedTime == 0 ? "undefined" : new DecimalFormat("#.00", symbols).format(througput));
            stopwatch.reset();
            stopwatch.start();
        }
    }
}
