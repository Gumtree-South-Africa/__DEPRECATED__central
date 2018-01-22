package com.ecg.replyts.core.api.util;

import java.util.Date;

public class CurrentClock implements Clock {
    @Override
    public Date now() {
        return new Date();
    }
}
