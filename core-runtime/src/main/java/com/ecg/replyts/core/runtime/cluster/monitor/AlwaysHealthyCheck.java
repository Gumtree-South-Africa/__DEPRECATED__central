package com.ecg.replyts.core.runtime.cluster.monitor;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;

/**
 * Always healthy, this implementation will be used if check are disabled.
 */
class AlwaysHealthyCheck implements Check {
    @Override
    public Result execute() throws Exception {
        return Result.createResult(Status.OK, Message.shortInfo("no check, always healthy"));
    }

    @Override
    public String getName() {
        return "OVERALL";
    }

    @Override
    public String getCategory() {
        return "RIAK";
    }

    @Override
    public String getSubCategory() {
        return getClass().getSimpleName();
    }
}
