package com.ecg.messagecenter.chat;

import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Element;

/**
 * Created by jaludden on 21/12/15.
 */
public class KijijiHrefAttributeProcessor extends KijijiAttributeProcessor {

    private final String siteUrl;

    public KijijiHrefAttributeProcessor(String siteUrl) {
        super("href");
        this.siteUrl = siteUrl;
    }

    @Override protected String getHostUrl() {
        return siteUrl;
    }

    @Override
    protected String getTargetAttributeName(Arguments arguments, Element element, String s) {
        return "href";
    }


    @Override public int getPrecedence() {
        return 10000;
    }
}
