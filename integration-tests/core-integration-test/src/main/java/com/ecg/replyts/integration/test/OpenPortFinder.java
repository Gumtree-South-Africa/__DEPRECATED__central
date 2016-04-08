package com.ecg.replyts.integration.test;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * Find an open port in a cross-VM environment, by locking a given port range with an atomic file write.
 */
public final class OpenPortFinder {
    private static final int RANGE_START = 4096;
    private static final int RANGE_SIZE = 1000;

    private static final String RANGE_CLAIM_TOPLEVEL_CHECK = "integration-tests";
    private static final String RANGE_CLAIM_TOPLEVEL_SUFFIX = "target" + File.separator + "_port_";

    private static File lockedFile = null;
    private static int lockedRangeStart = -1;
    private static int lastClaimedPort = -1;

    static {
        File parent = new File("pom.xml").getAbsoluteFile();

        do {
            parent = parent.getParentFile();
        } while (!(new File(parent, RANGE_CLAIM_TOPLEVEL_CHECK)).isDirectory());

        for (int i = RANGE_START; i < 65535; i += RANGE_SIZE) {
            File attemptedLock = new File(parent.getAbsolutePath() + File.separator + RANGE_CLAIM_TOPLEVEL_SUFFIX + i);

            if (!attemptedLock.getParentFile().exists()) {
                attemptedLock.getParentFile().mkdir();
            }

            try {
                if (attemptedLock.createNewFile()) {
                    lockedFile = attemptedLock;
                    lockedRangeStart = i;

                    break;
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to check (potentially preexisting) port lock file");
            }
        }

        if (lockedFile == null) {
            throw new IllegalStateException("Unable to claim a port range");
        }

        lockedFile.deleteOnExit();
    }

    public synchronized static int findFreePort() {
        int last = lastClaimedPort == -1 ? lockedRangeStart - 1 : lastClaimedPort;

        for (int testPort = last + 1; testPort < lockedRangeStart + RANGE_SIZE; testPort++) {
            if (available(testPort)) {
                lastClaimedPort = testPort;

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
    private static boolean available(int port) {
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