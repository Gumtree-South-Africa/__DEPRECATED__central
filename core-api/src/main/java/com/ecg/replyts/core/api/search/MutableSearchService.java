package com.ecg.replyts.core.api.search;

import java.time.LocalDate;

/**
 * allows deletion of items in the search index
 */
public interface MutableSearchService {

    /**
     * deletes all messages in the search index which were modified in the specified range.
     */
    void deleteModifiedAt(LocalDate from, LocalDate to);
}
