package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


final class SwitchLogger {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchLogger.class);

    private SwitchLogger() {
    }

    public static void log(String checkName, Optional<Result> from, Result to) {
        if (!from.isPresent()) {
            if (to.status() != Status.OK) {
                LOG.info("Check [{}] first run with Result {} ({})", checkName, to.status(), to.value());
            }
            return;
        }

        LOG.info("Check [{}] switched from [{}] to [{}]. Message: {} ", checkName, from.get().status(), to.status(), to.value().getShortInfo());

        Message message = to.value();
        if (to.status() != Status.OK) {
            if (message.hasCause()) {
                LOG.warn(message.getShortInfo(), message.getCause());
            } else {
                LOG.warn(message.getDetails());
            }
        }
    }

}
