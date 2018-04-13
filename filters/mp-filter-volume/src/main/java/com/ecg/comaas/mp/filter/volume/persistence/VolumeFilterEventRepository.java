package com.ecg.comaas.mp.filter.volume.persistence;

/**
 * A service that tracks email events per user.
 */
public interface VolumeFilterEventRepository {

    /**
     * Persist the event that some user send an email.
     *
     * @param userId sender of the mail
     * @param ttlInSeconds Time To Live of the event (>0)
     */
    void record(String userId, int ttlInSeconds);

    /**
     * Counts the number of email events within the last {@literal maxAgeInSecond} seconds.
     *
     * @param userId sender to count emails events for
     * @param maxAgeInSecond the amount of second in the past to count email events for
     * @return number of mail events in the last {@literal maxAgeInSecond} seconds
     */
    int count(String userId, int maxAgeInSecond);

}