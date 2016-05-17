package com.ecg.messagecenter;

import com.ecg.replyts.core.runtime.ReplyTS;

public class MessageCenterMain {

    public static void main(String[] args) throws Exception {
        // only working if inside pom.xml replyts-core is NOT marked as <scope>provided</scope>
        // only for local testing -> NEVER EVER checkin stuff with <scope>compile</scope>
        ReplyTS.main(args);
    }
}
