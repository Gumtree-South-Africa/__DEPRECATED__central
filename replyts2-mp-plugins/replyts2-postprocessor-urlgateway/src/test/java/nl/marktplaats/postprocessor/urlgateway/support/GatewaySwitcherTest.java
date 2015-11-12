package nl.marktplaats.postprocessor.urlgateway.support;

import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Properties;

import static java.util.Arrays.asList;
import static junit.framework.Assert.*;

/**
 * @author Erik van Oosten
 */
public class GatewaySwitcherTest {


    @Test
    public void testRedirectUrlToGateway() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://gateway.marktplaats.nl?url=[url]", asList("*.marktplaats.nl"));
        GatewaySwitcher gatewaySwitcher = new GatewaySwitcher(config);

        assertTrue(gatewaySwitcher.redirectUrlToGateway("http://www.google.nl"));
        assertTrue(gatewaySwitcher.redirectUrlToGateway("http://marktplaats.nl"));
        assertFalse(gatewaySwitcher.redirectUrlToGateway("http://verkoop.marktplaats.nl"));

        assertTrue(gatewaySwitcher.redirectUrlToGateway("http://www.google.nl/test?q=marktplaats.nl"));
        assertTrue(gatewaySwitcher.redirectUrlToGateway("http://marktplaats.nl/ad/12334333"));
        assertFalse(gatewaySwitcher.redirectUrlToGateway("http://verkoop.marktplaats.nl/ad/12334333"));

        assertTrue(gatewaySwitcher.redirectUrlToGateway("https://www.google.nl/test?q=marktplaats.nl"));
        assertTrue(gatewaySwitcher.redirectUrlToGateway("https://marktplaats.nl/ad/12334333"));
        assertFalse(gatewaySwitcher.redirectUrlToGateway("https://verkoop.marktplaats.nl/ad/12334333"));

        assertTrue(gatewaySwitcher.redirectUrlToGateway("file://www.google.nl/test?q=marktplaats.nl"));
        assertTrue(gatewaySwitcher.redirectUrlToGateway("file://marktplaats.nl/ad/12334333"));
        assertFalse(gatewaySwitcher.redirectUrlToGateway("file://verkoop.marktplaats.nl/ad/12334333"));

        assertTrue(gatewaySwitcher.redirectUrlToGateway("ftp://www.google.nl/test?q=marktplaats.nl"));
        assertTrue(gatewaySwitcher.redirectUrlToGateway("ftp://marktplaats.nl/ad/12334333"));
        assertFalse(gatewaySwitcher.redirectUrlToGateway("ftp://verkoop.marktplaats.nl/ad/12334333"));
    }

    @Test
    public void testUrlInMiddle() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://abc.com/def[url]ghi", new ArrayList<>());
        GatewaySwitcher gatewaySwitcher = new GatewaySwitcher(config);

        assertEquals("http://abc.com/defhttp%3A%2F%2Fhello.com%2Fghi", gatewaySwitcher.rewrite("http://hello.com/"));
    }

    @Test
    public void testUrlNotIncluded() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://abc.com/", new ArrayList<>());
        GatewaySwitcher gatewaySwitcher = new GatewaySwitcher(config);

        assertEquals("http://abc.com/http%3A%2F%2Fhello.com%2F", gatewaySwitcher.rewrite("http://hello.com/"));
    }
}
