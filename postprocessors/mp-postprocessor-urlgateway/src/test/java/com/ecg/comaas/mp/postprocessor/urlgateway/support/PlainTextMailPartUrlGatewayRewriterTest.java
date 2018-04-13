package com.ecg.comaas.mp.postprocessor.urlgateway.support;

import com.ecg.comaas.mp.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import com.ecg.comaas.mp.postprocessor.urlgateway.support.GatewaySwitcher;
import com.ecg.comaas.mp.postprocessor.urlgateway.support.PlainTextMailPartUrlGatewayRewriter;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Erik van Oosten
 */
public class PlainTextMailPartUrlGatewayRewriterTest {

    private GatewaySwitcher gatewaySwitcher;
    private PlainTextMailPartUrlGatewayRewriter underTest;

    @Before
    public void setUp() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig("http://gateway.marktplaats.nl/?url=[url]", asList("*.marktplaats.nl", "test.ebay.com"));
        gatewaySwitcher = new GatewaySwitcher(config);
        underTest = new PlainTextMailPartUrlGatewayRewriter();
    }

    @Test
    public void testRewriteUrls_doNotRewriteEmails() throws Exception {
        assertEquals(
                "mail@test.ebay.com",
                underTest.rewriteUrls("mail@test.ebay.com", gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_doNotSkipParentDomain() throws Exception {
        assertEquals(
                "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fmarktplaats.nl",
                underTest.rewriteUrls(
                        "http://marktplaats.nl",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_otherDomain() throws Exception {
        assertEquals(
                "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\n" +
                        "http://gateway.marktplaats.nl/?url=https%3A%2F%2Fwww.google.nl\n" +
                        "word http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl, word\n" +
                        "word,http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl,word\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl.\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl,\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl!\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl?\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl:\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl;\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl-\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq.\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq,\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq!\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq?\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq:\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.google.nl%2Fpath%2F%3Fq%3Dq;\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.test.%E0%A4%AD%E0%A4%BE%E0%A4%B0%E0%A4%A4\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Ftest.xn--h2brj9c\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.simple.nl%2F%3Fa%3D%28a%2Bb%29)\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fwww.simple.nl%2F%3Fa%3D%28a%2Bb%29\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2Fsimple.com\n" +
                        "http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\n" +
                        "http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\n" +
                        "http://gateway.marktplaats.nl/?url=ftp%3A%2F%2Fsimple.com%2Ffile\n" +
                        "http://gateway.marktplaats.nl/?url=http%3A%2F%2FSIMPLE.COM",
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
                                "www.simple.nl/?a=(a+b))\n" +
                                "http:/www.simple.nl/?a=(a+b)\n" +
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
    public void testRewriteUrls_skipDomain() throws Exception {
        assertEquals(
                "http://test.ebay.com",
                underTest.rewriteUrls(
                        "http://test.ebay.com",
                        gatewaySwitcher));
    }

    @Test
    public void testRewriteUrls_skipSubDomain() throws Exception {
        assertEquals(
                "http://www.marktplaats.nl",
                underTest.rewriteUrls(
                        "http://www.marktplaats.nl",
                        gatewaySwitcher));
    }
}
