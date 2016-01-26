package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;

import java.util.Map;
import java.util.Map.Entry;

import static java.lang.String.format;


/**
 * Encapsulate the results from the single checks. Provide methods to request the over all success an a
 * {@link #toString()} to format the results.
 *
 * @author smoczarski
 */
abstract class AggregatedCheckResult {

    public static Result aggregateResults(Map<String, Result> results) {
        Status cumulatedStatus = Status.OK;
        StringBuilder builder = new StringBuilder();
        for (Entry<String, Result> result : results.entrySet()) {
            Result checkResult = result.getValue();
            if (checkResult.status().ordinal() > cumulatedStatus.ordinal()) {
                cumulatedStatus = checkResult.status();
            }
            if (!checkResult.status().equals(Status.OK)) {
                builder.append(format("[%s(%s)", result.getKey(), checkResult.status()));
                if (checkResult.hasValue()) {
                    builder.append(format(":%s]", checkResult.value().toString()));
                } else {
                    builder.append("]");
                }
            }
        }
        String m = builder.toString();
        if (m.trim().isEmpty()) m = "All OK";
        return Result.createResult(cumulatedStatus, Message.detailed("", m));
    }

}
