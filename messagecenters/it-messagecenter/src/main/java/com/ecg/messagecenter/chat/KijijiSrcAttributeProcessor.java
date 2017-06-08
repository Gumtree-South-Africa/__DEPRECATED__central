package com.ecg.messagecenter.chat;

import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Element;

/**
 * Created by jaludden on 22/12/15.
 */
public class KijijiSrcAttributeProcessor extends KijijiAttributeProcessor {

    private String cdnUrl;

    public KijijiSrcAttributeProcessor(String cdnUrl) {
        super("src");
        this.cdnUrl = cdnUrl;
    }

    @Override
    protected String getTargetAttributeName(Arguments arguments, Element element, String s) {
        return "src";
    }

    @Override public int getPrecedence() {
        return 10001;
    }

    @Override protected String getHostUrl() {
        return cdnUrl;
    }
}
