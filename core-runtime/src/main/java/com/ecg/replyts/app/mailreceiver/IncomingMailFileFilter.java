package com.ecg.replyts.app.mailreceiver;

import java.io.File;
import java.io.FileFilter;

/**
 * file filter that will filter a folders files that contain with a specific prefix.
 *
 * @author mhuttar
 */
class IncomingMailFileFilter implements FileFilter {

    private final String requiredPrefix;

    public IncomingMailFileFilter(String requiredPrefix) {
        this.requiredPrefix = requiredPrefix;
    }

    @Override
    public boolean accept(File f) {
        return (f.isFile() && f.getName().startsWith(requiredPrefix));
    }

}
