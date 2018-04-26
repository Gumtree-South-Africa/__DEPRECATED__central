package com.ecg.messagecenter.it.chat;

import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by jaludden on 21/12/15.
 */
public class KijijiDialect extends AbstractDialect {

    private String siteUrl;
    private String cdnUrl;

    public KijijiDialect(String siteUrl, String cdnUrl) {
        this.siteUrl = siteUrl;
        this.cdnUrl = cdnUrl;
    }

    @Override public String getPrefix() {
        return "ki";
    }

    @Override public Set<IProcessor> getProcessors() {
        Set<IProcessor> processors = new HashSet<>();
        processors.add(new KijijiHrefAttributeProcessor(siteUrl));
        processors.add(new KijijiSrcAttributeProcessor(cdnUrl));
        return processors;
    }
}
