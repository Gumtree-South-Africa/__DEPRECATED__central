package nl.marktplaats.postprocessor.urlgateway.support;

import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Erik van Oosten
 */
public class HtmlMailPartUrlGatewayRewriterTest {

    private GatewaySwitcher gatewaySwitcher;
    private HtmlMailPartUrlGatewayRewriter underTest;

    @Before
    public void setUp() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig();
        config.setGatewayUrl("http://gateway.marktplaats.nl/?url=[url]");
        config.setSkipDomains(asList("*.marktplaats.nl", "test.ebay.com"));
        gatewaySwitcher = new GatewaySwitcher(config);
        underTest = new HtmlMailPartUrlGatewayRewriter();
    }

    @Test
    public void testRewriteUrls_plain_otherDomain() throws Exception {
        assertEquals(
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=https%3A%2F%2Fwww.google.nl\">https://www.google.nl</a>\n" +
                "word <a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>, word\n" +
                "word,<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>,word\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>.\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>,\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>!\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>?\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>:\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>;\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>-\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>.\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>,\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>!\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>?\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>:\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">www.google.nl/path/?q=q</a>;\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.test.%E0%A4%AD%E0%A4%BE%E0%A4%B0%E0%A4%A4\">www.test.भारत</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.xn--h2brj9c\">http://test.xn--h2brj9c</a>\n" +
                "word <a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.xn--h2brj9c\">http://test.xn--h2brj9c</a> word\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.simple.nl%2F%3Fa%3D%28a%2Bb%29\">www.simple.nl/?a=(a+b)</a>)\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.nl%2F%3Fa%3D%28a%2Bb%29\">http://simple.nl/?a=(a+b)</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http:/simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http:simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">ftp://simple.com/file</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">ftp:/simple.com/file</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">ftp:simple.com/file</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2FSIMPLE.COM\">http://SIMPLE.COM</a>",
                underTest.rewriteUrls(
                        "http://www.google.nl\n" +
                        "https://www.google.nl\n" +
                        "word http://www.google.nl, word\n" +
                        "word,http://www.google.nl,word\n" +
                        "www.google.nl\n" +
                        "www.google.nl.\n" +
                        "www.google.nl,\n" +
                        "www.google.nl!\n" +
                        "www.google.nl?\n" +
                        "www.google.nl:\n" +
                        "www.google.nl;\n" +
                        "www.google.nl-\n" +
                        "www.google.nl/path/?q=q\n" +
                        "www.google.nl/path/?q=q.\n" +
                        "www.google.nl/path/?q=q,\n" +
                        "www.google.nl/path/?q=q!\n" +
                        "www.google.nl/path/?q=q?\n" +
                        "www.google.nl/path/?q=q:\n" +
                        "www.google.nl/path/?q=q;\n" +
                        "www.test.भारत\n" +
                        "http://test.xn--h2brj9c\n" +
                        "word http://test.xn--h2brj9c word\n" +
                        "www.simple.nl/?a=(a+b))\n" +
                        "http://simple.nl/?a=(a+b)\n" +
                        "http://simple.com\n" +
                        "http:/simple.com\n" +
                        "http:simple.com\n" +
                        "ftp://simple.com/file\n" +
                        "ftp:/simple.com/file\n" +
                        "ftp:simple.com/file\n" +
                        "http://SIMPLE.COM",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_plain_regression() throws Exception {
        assertEquals(
                "<a href=\"http://gateway.marktplaats.nl/?url=https%3A%2F%2Fi.marktplaats.com%2F00%2Fs%2FMzg0WDUxMg%3D%3D%2F%24\">https://i.marktplaats.com/00/s/Mzg0WDUxMg==/$</a>",
                underTest.rewriteUrls("https://i.marktplaats.com/00/s/Mzg0WDUxMg==/$", gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_htmlLink_otherDomain() throws Exception {
        assertEquals(
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">https://www.google.nl</a>\n" +
                "word <a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>, word\n" +
                "word,<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>,word\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.nl\">google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.%E0%A4%AD%E0%A4%BE%E0%A4%B0%E0%A4%A4\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.xn--h2brj9c\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.nl%2F%3Fa%3D%28a%2Bb%29\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>",
                underTest.rewriteUrls(
                        "<a href=\"http://www.google.nl\">google.nl</a>\n" +
                        "<a href=\"http://www.google.nl\">https://www.google.nl</a>\n" +
                        "word <a href=\"http://www.google.nl\">http://www.google.nl</a>, word\n" +
                        "word,<a href=\"http://www.google.nl\">http://www.google.nl</a>,word\n" +
                        "<a href=\"google.nl\">google.nl</a>\n" +
                        "<a href=\"www.google.nl\">www.google.nl</a>\n" +
                        "<a href=\"http://www.google.nl/path/?q=q\">link</a>\n" +
                        "<a href=\"http://test.भारत\">link</a>\n" +
                        "<a href=\"http://test.xn--h2brj9c\">link</a>\n" +
                        "<a href=\"http://simple.nl/?a=(a+b)\">link</a>\n" +
                        "<a href=\"ftp://simple.com/file\">link</a>\n" +
                        "<a href=\"ftp:/simple.com/file\">link</a>\n" +
                        "<a href=\"ftp:simple.com/file\">link</a>",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_htmlLink_keepNonHttpSchemas() throws Exception {
        assertEquals(
                "<a href=\"mailto:test@example.com\">t@e.com</a>\n" +
                "<a href=\"tel:123456789\">987654321</a>\n" +
                "<a href=\"wlmailhtml:123456789\">987654321</a>\n" +
                "<a href=\"skype:handle?call\">skype me!</a>\n",
                underTest.rewriteUrls(
                        "<a href=\"mailto:test@example.com\">t@e.com</a>\n" +
                        "<a href=\"tel:123456789\">987654321</a>\n" +
                        "<a href=\"wlmailhtml:123456789\">987654321</a>\n" +
                        "<a href=\"skype:handle?call\">skype me!</a>\n",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_htmlLink_dangerousNonHttpSchemas() throws Exception {
        assertEquals(
                "<a>klik me!</a>\n" +
                "<a>klik me!</a>",
                underTest.rewriteUrls(
                        "<a href=\"javascript:alert('hi')\">klik me!</a>\n" +
                                "<a href=\"about:blank\">klik me!</a>",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_mixed_otherDomain() throws Exception {
        assertEquals(
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">https://www.google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "word <a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>, word\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "word,<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">http://www.google.nl</a>,word\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fgoogle.nl\">google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\">www.google.nl</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.%E0%A4%AD%E0%A4%BE%E0%A4%B0%E0%A4%A4\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.xn--h2brj9c\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.nl%2F%3Fa%3D%28a%2Bb%29\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\">link</a>\n" +
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\">http://simple.com</a>",
                underTest.rewriteUrls(
                        "http://simple.com\n" +
                        "<a href=\"http://www.google.nl\">google.nl</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"http://www.google.nl\">https://www.google.nl</a>\n" +
                        "http://simple.com\n" +
                        "word <a href=\"http://www.google.nl\">http://www.google.nl</a>, word\n" +
                        "http://simple.com\n" +
                        "word,<a href=\"http://www.google.nl\">http://www.google.nl</a>,word\n" +
                        "http://simple.com\n" +
                        "<a href=\"google.nl\">google.nl</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"www.google.nl\">www.google.nl</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"http://www.google.nl/path/?q=q\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"http://test.भारत\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"http://test.xn--h2brj9c\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"http://simple.nl/?a=(a+b)\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"ftp://simple.com/file\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"ftp:/simple.com/file\">link</a>\n" +
                        "http://simple.com\n" +
                        "<a href=\"ftp:simple.com/file\">link</a>\n" +
                        "http://simple.com",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_skipDomain() throws Exception {
        assertEquals(
                "<a href=\"http://test.ebay.com\">marktplaats.nl</a>",
                underTest.rewriteUrls(
                        "<a href=\"http://test.ebay.com\">marktplaats.nl</a>",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_skipSubDomain() throws Exception {
        assertEquals(
                "<a href=\"http://www.marktplaats.nl\">marktplaats.nl</a>",
                underTest.rewriteUrls(
                        "<a href=\"http://www.marktplaats.nl\">marktplaats.nl</a>",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_doNotSkipParentDomain() throws Exception {
        assertEquals(
                "<a href=\"http://gateway.marktplaats.nl/?url=http%3A%2F%2Fmarktplaats.nl\">marktplaats.nl</a>",
                underTest.rewriteUrls(
                        "<a href=\"http://marktplaats.nl\">marktplaats.nl</a>",
                        gatewaySwitcher));
    }

}
