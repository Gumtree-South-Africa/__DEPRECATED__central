package nl.marktplaats.filter.volume.measure;


import nl.marktplaats.filter.volume.VolumeFilterConfiguration;

/**
 * Definition of a service that is able to track sending volume information on
 * user basis for different platforms. This service is in charge of keeping a
 * time-based record of a user's mail sending history so that it can tell, if
 * specific {@link VolumeRule}s are violated.
 *
 * @author huttar
 */
public interface VolumeTrackingService {

    /**
     * Informs the Service that a user has send a mail via a specific platform.
     *
     * @param userId     sender of the mail
     */
    public abstract void record(String userId);

    /**
     * Checks if a user has violated a specific rule by comparing the maximum
     * amount of mails he is allowed to send in a time frame with the actual
     * amount of mails that he had sent.
     *
     * @param user     id to check
     * @param rule     rule to check
     * @return whether the user has violated this rule
     */
    public abstract boolean violates(String user, VolumeFilterConfiguration.VolumeRule rule);

    /**
     * Will be invoked regularly by a cronjob to remove considerably old Volume data (older than a day)
     */
    public abstract void cleanup();

}