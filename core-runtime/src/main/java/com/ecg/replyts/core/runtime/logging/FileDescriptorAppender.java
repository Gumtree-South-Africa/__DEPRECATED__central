package com.ecg.replyts.core.runtime.logging;

import ch.qos.logback.core.OutputStreamAppender;

import java.io.OutputStream;

public class FileDescriptorAppender<E> extends OutputStreamAppender<E> {

    protected FileDescriptorTarget target = FileDescriptorTarget.FileDescriptorOut;

    public void setTarget(String value) {
        target = FileDescriptorTarget.findByName(value.trim());
    }

    public String getTarget() {
        return target.getName();
    }

    @Override
    public void start() {
        OutputStream targetStream = target.getStream();
        setOutputStream(targetStream);
        super.start();
    }
}
