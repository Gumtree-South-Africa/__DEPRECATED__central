package com.ecg.de.ebayk.messagecenter.chat;

import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.context.VariablesMap;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor;
import org.thymeleaf.processor.attr.AbstractTextChildModifierAttrProcessor;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.standard.processor.attr.AbstractStandardSingleAttributeModifierAttrProcessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
