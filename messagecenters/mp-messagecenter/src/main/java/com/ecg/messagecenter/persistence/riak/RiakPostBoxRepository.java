package com.ecg.messagecenter.persistence.riak;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

public interface RiakPostBoxRepository {

    /**
     * Fetches a postbox by it's id.
     *
     * @param postBoxId id of postbox
     * @return a postbox object, even if it's a new postbox
     */
    PostBox getPostBox(String postBoxId);

    Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId);

    /**
     * @param postBoxId id of postbox
     * @return unread messages and unread conversation counts for postbox, both are 0 when postbox is unknown
     */
    PostBoxUnreadCounts getUnreadCounts(String postBoxId);

    void write(PostBox postBox);

    void cleanupLongTimeUntouchedPostBoxes(DateTime time);
}
