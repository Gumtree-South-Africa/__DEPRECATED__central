package com.ebay.ecg.australia.replyts.idreplacer;

import java.io.Serializable;

/**
 * @author mdarapour
 */
public class IdReplacerConfig implements Serializable {
    private int order;

    public IdReplacerConfig(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
