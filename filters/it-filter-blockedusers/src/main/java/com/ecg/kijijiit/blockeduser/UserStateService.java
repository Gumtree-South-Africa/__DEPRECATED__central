package com.ecg.kijijiit.blockeduser;

/**
 * Created by ddallemule on 2/10/14.
 */
public interface UserStateService {

    /**
     * given an email address says if a user is black listed or not
     *
     * @param userEmail user email address
     * @return true if the user is blacklisted otherwise false
     */
    boolean isBlocked(String userEmail) throws Exception;

}
