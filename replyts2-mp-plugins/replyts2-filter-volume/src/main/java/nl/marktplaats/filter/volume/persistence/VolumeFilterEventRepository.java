package nl.marktplaats.filter.volume.persistence;


import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;

/**
 * Definition of a service that is able to track sending volume information on
 * user basis for different platforms. This service is in charge of keeping a
 * time-based record of a user's mail sending history so that it can tell, if
 * specific {@link VolumeRule}s are violated.
 *
 * @author huttar
 */
public interface VolumeFilterEventRepository {

    /**
     * Informs the Service that a user has send a mail via a specific platform.
     *
     * @param userId  sender of the mail
     */
    void record(String userId, int ttlInSeconds);

    /**
     * Returns number of events after specific time for user
     * @param userId     id to check
     * @param maxAgeInSecond  millisec
     * @return number of events after specific time for user
     */
    int count(String userId, int maxAgeInSecond);

}