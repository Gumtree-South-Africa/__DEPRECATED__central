package com.ecg.de.kleinanzeigen.replyts.linkproxy;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EscaperTest {
    Escaper esc = new Escaper("http://escape.com/escape?to=%s", ImmutableSet.of("kleinanzeigen.ebay.de", "ebay.com", "ebay.de", "mobile.de"));


    @Test
    public void wrapsUrlToProxy() {
        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%3Fabc%3D123", esc.escapePlaintext("hello http://fraudster.com?abc=123"));
    }

    @Test
    public void wrapsAllUrls() {
        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%3Fabc%3D123 this is my 2nd link http://escape.com/escape?to=http%3A%2F%2Fwww.heise.de%3Fkjhdsfjkh%3Djkhdsfj click me.", esc.escapePlaintext("hello http://fraudster.com?abc=123 this is my 2nd link http://www.heise.de?kjhdsfjkh=jkhdsfj click me."));
    }

    @Test
    public void wrapHtmlLink() {
        assertEquals("<a href=\"http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%3Fabc%3D123\">click</a>", esc.escapePlaintext("<a href=\"http://fraudster.com?abc=123\">click</a>"));
    }

    @Test
    public void enclosingTeesIgnoreOnWhitelist() {
        assertEquals("<a href=\"http://presse.ebay.de\">&lt;http://presse.ebay.de&gt;</A>&nbsp;&nbsp;=20", esc.escapeHtml("<a href=\"http://presse.ebay.de\">&lt;http://presse.ebay.de&gt;</A>&nbsp;&nbsp;=20"));
    }

    @Test
    public void doNotWrapImageHtmlLink() {
        assertEquals("<img src=\"http://foo.com/bar.jpg\">",
                esc.escapeHtml("<img src=\"http://foo.com/bar.jpg\">"));
    }

    @Test
    public void doNotWrapNestedImageHtmlLink() {
        assertEquals(
                "<a href=\"http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%3Fabc%3D123\"><img src=\"http://foo.com/bar.jpg\"></a>",
                esc.escapeHtml("<a href=\"http://fraudster.com?abc=123\"><img src=\"http://foo.com/bar.jpg\"></a>"));
    }

    @Test
    public void doNotEscapeWhitelistDomains() {
        assertEquals("hello http://www.kleinanzeigen.ebay.de#anchor", esc.escapePlaintext("hello http://www.kleinanzeigen.ebay.de#anchor"));
        assertEquals("hello http://www.kleinanzeigen.ebay.de?param=value", esc.escapePlaintext("hello http://www.kleinanzeigen.ebay.de?param=value"));
        assertEquals("hello http://www.kleinanzeigen.ebay.de/anzeigen", esc.escapePlaintext("hello http://www.kleinanzeigen.ebay.de/anzeigen"));
    }

    @Test
    public void wrapPlaintextLinksInHtml() {
        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%3Fabc%3D123", esc.escapeHtml("hello http://fraudster.com?abc=123"));
    }

    @Test
    public void wrapPlaintextLinksWithSubpathsInHtml() {
        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Ffraudster.com%2Ffoo", esc.escapeHtml("hello http://fraudster.com/foo"));
    }

    @Test
    public void wrapsUrlEncodedUrlInHtml() {
        assertEquals("http://escape.com/escape?to=http%3A%2F%2Fwww.facebook.com", esc.escapeHtml("http:&#x2F;&#x2F;www.facebook.com"));

    }

    @Test
    public void proxyFakeDomains() {

        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Fwww.kleinanzeigen.ebay.de.ru%2Fanzeigen", esc.escapePlaintext("hello http://www.kleinanzeigen.ebay.de.ru/anzeigen"));
    }


    @Test
    public void proxyDomainsCaseInsensitive() {

        assertEquals("hello http://www.kleinanzeigen.EBAY.DE/anzeigen", esc.escapePlaintext("hello http://www.kleinanzeigen.EBAY.DE/anzeigen"));
    }

    @Test
    public void escapesEncodedSlashesInHtml() {
        assertEquals("http://escape.com/escape?to=http%3A%2F%2Fwww.facebook.com", esc.escapeHtml("http:&#x2F;&#x2F;www.facebook.com"));

    }

    @Test
    public void skipsEncodedAllowedDomainsInHtml() {
        assertEquals("hello http://www.kleinanzeigen.EBAY.DE/anzeigen", esc.escapeHtml("hello http:&#x2F;&#x2F;www.kleinanzeigen.EBAY.DE/anzeigen"));

    }

    @Test
    public void proxyFakeDomains2() {

        assertEquals("hello http://escape.com/escape?to=http%3A%2F%2Fwww.kleinanzeigen.fakeebay.de%2Fanzeigen", esc.escapePlaintext("hello http://www.kleinanzeigen.fakeebay.de/anzeigen"));
    }
}
