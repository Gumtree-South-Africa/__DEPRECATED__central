package com.ebay.ecg.replyts.robot.api;

import com.ecg.replyts.integration.test.OpenPortFinder;

/**
 * Created by mdarapour.
 */
public class RabbitMQRunner {
    private int port = 5672;

    public void start() {
        //port = OpenPortFinder.findFreePort();
    }

    public void stop() throws Exception {

    }

    public int getPort() {
        return port;
    }
}
