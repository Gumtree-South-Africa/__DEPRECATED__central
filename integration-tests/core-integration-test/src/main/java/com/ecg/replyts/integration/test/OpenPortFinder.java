package com.ecg.replyts.integration.test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public final class OpenPortFinder {

    private static final int MIN = 4096;
    private static final int STEP = (int) (Math.random() * 100);
    private static int last = MIN;

    private OpenPortFinder() {
    }

    /**
     * finds a free, non privileged port and returns it. does so by guessing port numbers. if fails, then throws IllegalStateException.
     *
     * @return free port number.
     */
    public static int findFreePort() {
        for (int i = 1; i < 20; i++) {
            int testPort = last + STEP * i;
            if (available(testPort)) {
                last = testPort;
                return testPort;
            }
        }
        throw new IllegalStateException("Could not manage to find a free port");
    }


    /**
     * Sourcecode from Apache Mina project<br/>
     * <p/>
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    public static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (ds != null) {
                ds.close();
            }
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    throw new IllegalStateException("should never happen", e);
                }
            }
        }


    }

}
