package com.ecg.messagecenter.ebayk.util;

/**
 * Wraps a Relative Url and adds the absolute path to the eBayK website (by default the common eBay Kleinanzeigen Base Path).
 * @author fsemrau
 */
public final class AbsoluteBackdoorServerUrl {


    public static final String BASE_URL = System.getProperty("application.backdoor.base.url", "https://cs.corp.ebay-kleinanzeigen.de/backdoor/");

    private final String relativeUrl;


    public AbsoluteBackdoorServerUrl(String relativeUrl) {
        this.relativeUrl = relativeUrl;
    }

    public String getUrl() {
        return BASE_URL + relativeUrl;
    }
}
