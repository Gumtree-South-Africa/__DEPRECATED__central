package com.ecg.replyts.core.api.search;

import com.google.common.collect.Range;
import org.joda.time.DateTime;

/**
 * allows deletion of items in the search index
 */
public interface MutableSearchService {

    /**
     * deletes all messages in the search index where the conversation was created in the specified range.
     */
    void delete(Range<DateTime> allConversationsCreatedBetween);
}
