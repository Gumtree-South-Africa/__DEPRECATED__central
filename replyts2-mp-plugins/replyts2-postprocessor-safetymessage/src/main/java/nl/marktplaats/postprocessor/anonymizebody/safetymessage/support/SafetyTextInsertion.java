package nl.marktplaats.postprocessor.anonymizebody.safetymessage.support;

/**
 * Created by reweber on 19/10/15
 */
public interface SafetyTextInsertion {

    /**
     * Inserts safety announcement in a string.
     *
     * @param content the content to change (not null)
     * @param safetyText the text to be added, typically in front of the existing content (not null)
     * @return the content with the added safetyText
     */
    String insertSafetyText(String content, String safetyText);

}
