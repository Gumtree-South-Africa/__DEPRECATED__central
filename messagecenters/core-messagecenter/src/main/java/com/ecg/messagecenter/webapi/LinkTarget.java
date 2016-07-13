package com.ecg.messagecenter.webapi;

import static com.google.common.base.Preconditions.checkNotNull;

class LinkTarget {
    private String href;

    LinkTarget(String href) {
        checkNotNull(href);
        this.href = href;
    }
}
