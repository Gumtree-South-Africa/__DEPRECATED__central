package com.ecg.de.ebayk.messagecenter.webapi;

import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;

class LinkTarget {
    private String href;

    LinkTarget(String href) {
        checkNotNull(href);
        this.href = href;
    }
}
