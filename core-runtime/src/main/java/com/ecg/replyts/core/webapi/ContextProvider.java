package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.Handler;

public interface ContextProvider {
    Handler create();

    void test();

    String getPath();
}
