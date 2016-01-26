package com.ecg.replyts.app.mailreceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

class ScanAndRename {
    private final File dropFolder;
    private final String searchForPrefix;
    private final String renameToPrefix;

    private static final Logger LOG = LoggerFactory.getLogger(ScanAndRename.class);

    public ScanAndRename(File dropFolder, String searchForPrefix, String renameToPrefix) {
        this.dropFolder = dropFolder;
        this.searchForPrefix = searchForPrefix;
        this.renameToPrefix = renameToPrefix;
    }

    public void execute() {
        for (File file : dropFolder.listFiles()) {
            if (file.getName().startsWith(searchForPrefix)) {
                File targetName = new File(file.getParent(), renameToPrefix + file.getName());
                LOG.info("Cleanup Drop Folder: Renaming {} to {}", file.getName(), targetName.getName());
                file.renameTo(targetName);
            }
        }

    }
}
