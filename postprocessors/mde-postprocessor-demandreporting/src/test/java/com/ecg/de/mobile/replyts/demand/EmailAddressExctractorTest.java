package com.ecg.de.mobile.replyts.demand;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EmailAddressExctractorTest {

    @Test
    public void extractsEmailAdress() {
        String header = "Klaus Meier <klaus@foo.web.de>";
        assertEquals("klaus@foo.web.de", EmailAddressExctractor.extractFromSmptHeader(header));
    }
}
