package com.ebay.ecg.australia.replyts.headers;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mdarapour
 */
public class HeaderInjectorPostprocessorConfig implements Serializable {
    private List<String> headersToInject;
    private int order;

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

    public void cleanup() {
        List<String> temp = new ArrayList<String>();
        for (String headerName : headersToInject) {
            if (headerName.startsWith(Mail.CUSTOM_HEADER_PREFIX)) {
                headerName = headerName.substring(Mail.CUSTOM_HEADER_PREFIX.length());
            }
            temp.add(headerName.toLowerCase());
        }
        this.headersToInject = temp;
    }
}
