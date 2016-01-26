package com.ecg.replyts.core.runtime;


/**
 * Registered as shutdown hook in the JVM to properly shut down ReplyTS. Used when ReplyTS is being shut down via a kill signal.
 *
 * @author mhuttar
 */
final class ApplicationShutdownHook extends Thread {


    private final ReplyTS replyts;

    public ApplicationShutdownHook(ReplyTS replyts) {
        this.replyts = replyts;
    }

    @Override
    public void run() {
        replyts.shutdown();
    }

}