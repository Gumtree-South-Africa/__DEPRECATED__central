package com.ecg.messagecenter.kjca.sync;

import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Diffing {

    private static final Logger LOG = LoggerFactory.getLogger(Diffing.class);

    public static void diffPostBox(Optional<PostBox> newbox, PostBoxDiff oldbox, String email) {
        if (LOG.isDebugEnabled()) {
            try {
                internalDiffPostBox(newbox, oldbox, email);
            } catch (Exception ex) {
                LOG.debug("DIFF: EXCEPTION: ", ex);
            }
        }
    }

    private static void internalDiffPostBox(Optional<PostBox> newbox, PostBoxDiff oldbox, String email) {
        if (!newbox.isPresent()) {
            LOG.debug("DIFF: NO-POSTBOX: E-mail: " + email);
            return;
        }

        PostBox newPostBox = newbox.get();
        PostBoxResponse oldPostBox = oldbox.getPostBoxResponse();

        int newCount = newPostBox.getConversationsTotalCount();
        int oldCount = oldPostBox.getConversations().size();
        if (newCount != oldCount) {
            LOG.debug("DIFF: COUNTS: E-mail: " + email + ", User-ID: " + newPostBox.getUserId() + ", NEW/OLD: " + newCount + "/" + oldCount);
            return;
        }

        int newUnread = newPostBox.getUnreadCounts().getNumUnreadConversations();
        int oldUnread = oldPostBox.getNumUnread();
        if (newUnread != oldUnread) {
            LOG.debug("DIFF: UNREAD: E-mail: " + email + ", User-ID: " + newPostBox.getUserId() + ", NEW/OLD: " + newUnread + "/" + oldUnread);
            return;
        }
    }
}
