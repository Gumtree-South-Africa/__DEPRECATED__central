package com.ecg.comaas.gtau.postprocessor.headerinjector;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public class HeaderInjectorPostprocessorConfig implements Serializable {

    private static final long serialVersionUID = -4335999688187094952L;

    private final List<String> headersToInject;
    private final int order;

    public HeaderInjectorPostprocessorConfig(String headers, int order) {
        headersToInject = Lists.newArrayList(Splitter.on(',').split(headers));
        this.order = order;
    }

    public List<String> getHeadersToInject() {
        return headersToInject;
    }

    public int getOrder() {
        return order;
    }
}
