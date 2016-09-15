package com.ecg.replyts.core.runtime.persistence;

import java.util.Optional;

/**
 * Repository for blocked users
 */

public interface BlockUserRepository {
    void blockUser(String blockerUserId, String userIdToBlock);

    void unblockUser(String blockerUserId, String userIdToUnblock);

    Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId2);

    boolean areUsersBlocked(String userId1, String userId2);

}
