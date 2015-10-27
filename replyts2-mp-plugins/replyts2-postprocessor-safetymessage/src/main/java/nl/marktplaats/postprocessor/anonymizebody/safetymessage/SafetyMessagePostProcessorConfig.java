package nl.marktplaats.postprocessor.anonymizebody.safetymessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by reweber on 19/10/15
 */
public class SafetyMessagePostProcessorConfig {

    /**
     * Safety text that is to be injected in e-mails towards the seller.
     * First entry will be for the first e-mail, second entry for the second e-mail, etc.
     * When the list is exhausted, no more text is injected.
     * When an entry is empty, no text is injected.
     *
     * Note: the texts are expected to be in HTML. For plain text mail parts the
     * link text will be removed (only the actual link will be used).
     */
    private List<String> safetyTextForSeller = new ArrayList<>();

    /**
     * Safety text that is to be injected in e-mails towards the buyer.
     * First entry will be for the first e-mail, second entry for the second e-mail, etc.
     * When the list is exhausted, no more text is injected.
     * When an entry is empty, no text is injected.
     *
     * Note: the texts are expected to be in HTML. For plain text mail parts the
     * link text will be removed (only the actual link will be used).
     */
    private List<String> safetyTextForBuyer = new ArrayList<>();

    public SafetyMessagePostProcessorConfig(List<String> safetyTextForSeller, List<String> safetyTextForBuyer) {
        this.safetyTextForSeller = safetyTextForSeller;
        this.safetyTextForBuyer = safetyTextForBuyer;
    }

    public List<String> getSafetyTextForSeller() {
        return safetyTextForSeller;
    }

    public List<String> getSafetyTextForBuyer() {
        return safetyTextForBuyer;
    }
}
