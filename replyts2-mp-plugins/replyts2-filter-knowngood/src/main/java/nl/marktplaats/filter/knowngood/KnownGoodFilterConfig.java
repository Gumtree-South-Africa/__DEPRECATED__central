package nl.marktplaats.filter.knowngood;

class KnownGoodFilterConfig {

    private static final String RESPONDER_GOOD_HEADER = "respondergood";
    private static final String INITIATOR_GOOD_HEADER = "initiatorgood";

    public String getResponderGoodHeader() {
        return RESPONDER_GOOD_HEADER;
    }

    public String getInitiatorGoodHeader() {
        return INITIATOR_GOOD_HEADER;
    }

}
