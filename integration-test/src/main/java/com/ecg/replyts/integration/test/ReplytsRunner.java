package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.runtime.ReplyTS;
import com.google.common.io.Files;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import java.io.File;
import java.util.List;

public final class ReplytsRunner {

    static final String DEFAULT_CONFIG_RESOURCE_DIRECTORY = "/integrationtest-conf";

    private final String configResourcePrefix;

    private ReplyTS replyTs;
    private int replytsHttpPort = -1;

    private File dropFolder;

    private Wiser wiser;

    public ReplytsRunner(String configResourcePrefix) {
        this.configResourcePrefix = configResourcePrefix;
    }

    public void start() throws Exception {
        if (replyTs != null) {
            return;
        }
        replytsHttpPort = OpenPortFinder.findFreePort();

        dropFolder = Files.createTempDir();
        System.setProperty("mailreceiver.filesystem.dropfolder", dropFolder.getAbsolutePath());

        int replytsSmtpOutPort = OpenPortFinder.findFreePort();
        System.setProperty("delivery.smtp.port", String.valueOf(replytsSmtpOutPort));

        wiser = new Wiser(replytsSmtpOutPort);
        wiser.start();

        ClassPathEnvironmentSupport environmentSupport = new ClassPathEnvironmentSupport(configResourcePrefix, replytsHttpPort);
        replyTs = new ReplyTS(environmentSupport);
    }

    public void stop() {
        replyTs.shutdown();
        deleteDirectory(dropFolder);
        wiser.stop();
    }

    public List<WiserMessage> getMessages() {
        return wiser.getMessages();
    }

    /**
     * @return port number on which ReplyTS Server is running. if no webserver started, -1,
     */
    public int getReplytsHttpPort() {
        return replytsHttpPort;
    }

    public File getDropFolder() {
        return dropFolder;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }

}
