package com.ecg.replyts.core.runtime.persistence;

import java.util.List;
import java.util.Optional;

public interface BlockUserRepository {
    void blockUser(String blockerUserId, String userIdToBlock);

    void unblockUser(String blockerUserId, String userIdToUnblock);

    Optional<BlockedUserInfo> getBlockedUserInfo(String userId1, String userId2);

    boolean areUsersBlocked(String userId1, String userId2);

    List<String> listBlockedUsers(String userId1);

    boolean isBlocked(String userId1, String userId2);
}
