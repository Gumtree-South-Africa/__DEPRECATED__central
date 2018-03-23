package com.ecg.replyts.core;

import org.springframework.context.ApplicationEvent;

public class ApplicationReadyEvent extends ApplicationEvent {

    public ApplicationReadyEvent(Object source) {
        super(source);
    }
}
