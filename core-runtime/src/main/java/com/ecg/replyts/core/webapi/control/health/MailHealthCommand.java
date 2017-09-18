package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.runtime.maildelivery.smtp.SmtpMailDeliveryCheck;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class MailHealthCommand extends AbstractHealthCommand {

    static final String COMMAND_NAME = "mail";
    private final SmtpMailDeliveryCheck smtpCheck;

    MailHealthCommand(SmtpMailDeliveryCheck smtpCheck) {
        this.smtpCheck = smtpCheck;
    }

    @Override
    public ObjectNode execute() {
        try {
            Result result = smtpCheck.execute();
            switch (result.status()) {
                case OK:
                    return up();
                default:
                    return down(result.value().getDetails());
            }
        } catch (Exception e) {
            return down(e.getMessage());
        }
    }

    @Override
    public String name() {
        return COMMAND_NAME;
    }
}
