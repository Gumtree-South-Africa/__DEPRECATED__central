package nl.marktplaats.postprocessor.urlgateway.support;

import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Erik van Oosten
 */
public class BugHtmlMailtoLinkReplacedTest {

    private GatewaySwitcher gatewaySwitcher;
    private HtmlMailPartUrlGatewayRewriter underTest;

    @Before
    public void setUp() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://gateway.marktplaats.nl/?url=[url]", asList(
                "*.marktplaats.nl", "marktplaats.custhelp.com",
                "images.emessaging.nl", "*.marktplaats.com",
                "www.websitevanhetjaar.nl", "www.marktplaatsmanieren.nl"
        ));
        gatewaySwitcher = new GatewaySwitcher(config);
        underTest = new HtmlMailPartUrlGatewayRewriter();
    }

    @Test
    public void testRewriteUrls_noRewrites() throws Exception {
        String mailContentIn = readUtf8Resource("/BugHtmlMailtoLinkReplaced_in.html");
        String mailContentExpected = readUtf8Resource("/BugHtmlMailtoLinkReplaced_expected.html");
        assertNotNull(mailContentIn, "Could not read resource");
        assertNotNull(mailContentExpected, "Could not read resource");

        assertEquals(mailContentExpected, underTest.rewriteUrls(mailContentIn, gatewaySwitcher));
    }

    private String readUtf8Resource(String resource) throws IOException {
        InputStream io = getClass().getResourceAsStream(resource);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[20480];
        int bytes;
        while ((bytes = io.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytes);
        }
        return new String(byteArrayOutputStream.toByteArray(), "UTF-8");
    }

}
