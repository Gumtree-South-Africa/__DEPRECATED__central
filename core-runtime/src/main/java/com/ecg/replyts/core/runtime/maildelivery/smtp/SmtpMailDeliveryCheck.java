package com.ecg.replyts.core.runtime.maildelivery.smtp;

import com.ecg.replyts.core.api.sanitychecks.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SmtpMailDeliveryCheck implements CheckProvider, Check {
    @Autowired
    private SmtpPing smtpPing;

    @Autowired
    private SmtpDeliveryConfig config;

    public Result execute() throws Exception {
        try {
            smtpPing.ping(config.getHost(), config.getPort(), config.getConnectTimeoutInMs());

            return Result.createResult(Status.OK, Message.shortInfo("SMTP connection established successfully"));
        } catch (Exception ex) {
            return Result.createResult(Status.CRITICAL, Message.fromException(ex));
        }
    }

    @Override
    public String getName() {
        return "SMTP-" + config.getHost();
    }

    @Override
    public String getCategory() {
        return "SMTP";
    }

    @Override
    public String getSubCategory() {
        return getClass().getSimpleName();
    }

    @Override
    public List<Check> getChecks() {
        return Collections.singletonList(this);
    }
}
