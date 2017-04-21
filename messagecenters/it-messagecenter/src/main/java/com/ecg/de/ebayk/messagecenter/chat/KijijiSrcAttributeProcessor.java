package com.ecg.de.ebayk.messagecenter.chat;

import org.thymeleaf.Arguments;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor;
import org.thymeleaf.standard.processor.attr.AbstractStandardSingleAttributeModifierAttrProcessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
