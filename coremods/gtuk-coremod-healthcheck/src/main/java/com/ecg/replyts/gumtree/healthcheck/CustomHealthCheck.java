package com.ecg.replyts.gumtree.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Status;

public class CustomHealthCheck extends HealthCheck {
    private Check check;

    public CustomHealthCheck(Check check) {
        this.check = check;
    }

    @Override
    protected Result check() throws Exception {
        com.ecg.replyts.core.api.sanitychecks.Result result = check.execute();
        if (result.status() == Status.OK) {
            String shortInfo = result.hasValue() ? result.value().getShortInfo() : "";
            return Result.healthy(shortInfo);
        } else {
            String shortInfo = result.hasValue() ? result.value().getShortInfo() : "";
            String details = result.hasValue() ? result.value().getDetails() : "";
            return Result.unhealthy(shortInfo, details);
        }
    }
}
