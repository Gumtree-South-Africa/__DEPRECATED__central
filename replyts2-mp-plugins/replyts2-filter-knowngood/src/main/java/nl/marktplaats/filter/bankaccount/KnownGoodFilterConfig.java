package nl.marktplaats.filter.bankaccount;

/**
 * Created by reweber on 16/10/15
 */
class KnownGoodFilterConfig {

    private static final String RESPONDER_GOOD_HEADER = "respondergood";
    private static final String INITIATOR_GOOD_HEADER = "initiatorgood";

    public final String getResponderGoodHeader() {
        return RESPONDER_GOOD_HEADER;
    }

    public final String getInitiatorGoodHeader() {
        return INITIATOR_GOOD_HEADER;
    }

}
