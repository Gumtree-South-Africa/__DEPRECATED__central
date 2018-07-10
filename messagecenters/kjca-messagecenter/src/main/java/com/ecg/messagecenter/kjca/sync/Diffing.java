package com.ecg.messagecenter.kjca.sync;

import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxListItemResponse;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Diffing {

    private static final Logger LOG = LoggerFactory.getLogger(Diffing.class);

    public static void diffPostBox(Optional<PostBox> newbox, com.ecg.messagecenter.core.persistence.simple.PostBox oldbox, String email) {
        if (LOG.isDebugEnabled()) {
            try {
                if (!newbox.isPresent() ) {
                    // MessageBox is empty, are there any valid conversations in MessageCenter?
                    if (oldbox.getConversationThreads().size() > 0) {
                        LOG.debug("DIFF: NO-POSTBOX: E-mail: " + email + " - " + oldbox.getConversationThreads());
                    }

                    return;
                }

                internalDiffPostBox(newbox.get(), oldbox, email);
            } catch (Exception ex) {
                LOG.debug("DIFF: EXCEPTION: ", ex);
            }
        }
    }

    private static void internalDiffPostBox(PostBox newbox,
                                            com.ecg.messagecenter.core.persistence.simple.PostBox<com.ecg.messagecenter.kjca.persistence.ConversationThread> oldbox,
                                            String email) {

        int newCount = newbox.getConversationsTotalCount();
        int oldCount = oldbox.getConversationThreads().size();
        if (newCount != oldCount) {
            List<String> newIds = newbox.getConversations().stream()
                    .map(ConversationThread::getId)
                    .collect(Collectors.toList());

            List<String> oldIds = oldbox.getConversationThreads().stream()
                    .map(com.ecg.messagecenter.kjca.persistence.ConversationThread::getConversationId)
                    .collect(Collectors.toList());

            LOG.debug("DIFF: COUNTS: E-mail: " + email + ", User-ID: " + newbox.getUserId() + ", NEW/OLD: " + newCount + "/" + oldCount + " | " + newIds + " | " + oldIds);
        }

        int newUnread = newbox.getUnreadCounts().getNumUnreadConversations();
        int oldUnread = oldbox.getUnreadConversations().size();
        if (newUnread != oldUnread) {
            LOG.debug("DIFF: UNREAD: E-mail: " + email + ", User-ID: " + newbox.getUserId() + ", NEW/OLD: " + newUnread + "/" + oldUnread);
        }
    }
}
