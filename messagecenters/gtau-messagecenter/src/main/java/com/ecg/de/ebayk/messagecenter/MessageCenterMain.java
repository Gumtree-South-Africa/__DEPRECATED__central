package com.ecg.de.ebayk.messagecenter;

import com.ecg.replyts.core.runtime.ReplyTS;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:17
 *
 * @author maldana@ebay.de
 */
public class MessageCenterMain {

    public static void main(String[] args) throws Exception {
        // only working if inside pom.xml replyts-core is NOT marked as <scope>provided</scope>
        // only for local testing -> NEVER EVER checkin stuff with <scope>compile</scope>
        ReplyTS.main(args);
    }
}
