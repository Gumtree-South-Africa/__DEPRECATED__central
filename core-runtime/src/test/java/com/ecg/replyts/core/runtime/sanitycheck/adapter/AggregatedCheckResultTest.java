package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class AggregatedCheckResultTest {

    @Test
    public void testStatus_ok() {

        Map<String, Result> results = new HashMap<String, Result>();
        results.put("name-1", Result.createResult(Status.OK, Message.shortInfo("message-1")));
        results.put("name-2", Result.createResult(Status.OK, Message.shortInfo("message-2")));

        Result result = AggregatedCheckResult.aggregateResults(results);
        assertEquals(Status.OK, result.status());
    }

    @Test
    public void testStatus_warning() {

        Map<String, Result> results = new HashMap<String, Result>();
        results.put("name-1", Result.createResult(Status.OK, Message.shortInfo("message-1")));
        results.put("name-2", Result.createResult(Status.WARNING, Message.shortInfo("message-2")));

        Result result = AggregatedCheckResult.aggregateResults(results);
        assertEquals(Status.WARNING, result.status());
    }

    @Test
    public void testStatus_critical() {

        Map<String, Result> results = new HashMap<String, Result>();
        results.put("name-1", Result.createResult(Status.OK, Message.shortInfo("message-1")));
        results.put("name-2", Result.createResult(Status.CRITICAL, Message.shortInfo("message-2")));
        results.put("name-3", Result.createResult(Status.WARNING, Message.shortInfo("message-3")));

        Result result = AggregatedCheckResult.aggregateResults(results);
        assertEquals(Status.CRITICAL, result.status());
    }

    @Test
    public void testToString() {

        Map<String, Result> results = new HashMap<String, Result>();
        results.put("name-1", Result.createResult(Status.OK, Message.shortInfo("message-1")));
        results.put("name-2", Result.createResult(Status.WARNING, Message.shortInfo("message-2")));

        Result result = AggregatedCheckResult.aggregateResults(results);
        assertEquals("[name-2(WARNING):message-2]", result.value().toString());
    }
}
