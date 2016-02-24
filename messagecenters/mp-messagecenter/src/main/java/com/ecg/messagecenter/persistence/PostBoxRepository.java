package com.ecg.messagecenter.persistence;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 * Created by pragone
 * Created on 21/10/15 at 12:35 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public interface PostBoxRepository {

    /**
     * Fetches a postbox by id.
     *
     * @param postBoxId the identifier of the postbox
     * @return a postbox object, even if it's a new postbox
     */
    PostBox byId(String postBoxId);

    void write(PostBox postBox);

    void cleanupLongTimeUntouchedPostBoxes(DateTime time);

    Optional<ConversationThread> getConversationThread(String postBoxId, String conversationId);

    void addReplaceConversationThread(String postBoxId, ConversationThread conversationThread);
}