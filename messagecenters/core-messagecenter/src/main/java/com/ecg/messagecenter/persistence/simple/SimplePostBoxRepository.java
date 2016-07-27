package com.ecg.messagecenter.persistence.simple;

import org.joda.time.DateTime;

import java.util.List;

public interface SimplePostBoxRepository {
    PostBox byId(String email);

    void write(PostBox postBox);

    // ugly but easier instead of changing PostBox model and persistence
    // we need to pass deletion-context so mutator works correctly, we need deleted-ids so that merging works
    void write(PostBox postBox, DeletionContext deletionContext);

    void cleanupLongTimeUntouchedPostBoxes(DateTime time);

    class DeletionContext {
        List<String> deletedIds;

        public DeletionContext(List<String> deletedIds) {
            this.deletedIds = deletedIds;
        }

        public List<String> getDeletedIds() {
            return deletedIds;
        }
    }
}

