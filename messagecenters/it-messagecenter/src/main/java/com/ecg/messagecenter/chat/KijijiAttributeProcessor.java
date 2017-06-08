package com.ecg.messagecenter.chat;

import org.thymeleaf.Arguments;
import org.thymeleaf.dom.Element;
import org.thymeleaf.standard.processor.attr.AbstractStandardSingleAttributeModifierAttrProcessor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jaludden on 22/12/15.
 */
public abstract class KijijiAttributeProcessor
                extends AbstractStandardSingleAttributeModifierAttrProcessor {

    public KijijiAttributeProcessor(String attributeName) {
        super(attributeName);
    }

    @Override
    protected ModificationType getModificationType(Arguments arguments, Element element, String s,
                    String s1) {
        return ModificationType.SUBSTITUTION;
    }

    @Override
    protected boolean removeAttributeIfEmpty(Arguments arguments, Element element, String s,
                    String s1) {
        return false;
    }

    @Override protected String getTargetAttributeValue(Arguments arguments, Element element,
                    String attributeName) {
        return getHostUrl() + replaceExpressions(element.getAttributeValue(attributeName),
                        arguments.getContext().getVariables());
    }


    protected String replaceExpressions(String attributeValue, Map<String, Object> variables) {
        Matcher m = Pattern.compile("\\$\\{(.*)\\}").matcher(attributeValue);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            Object value = variables.get(m.group(1));

            if (value == null) {
                value = "";
            }

            m.appendReplacement(sb, value.toString());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    protected abstract String getHostUrl();

}
