package com.ecg.comaas.gtau.postprocessor.idreplacer;

import java.io.Serializable;

public class IdReplacerConfig implements Serializable {
    private int order;

    public IdReplacerConfig(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
