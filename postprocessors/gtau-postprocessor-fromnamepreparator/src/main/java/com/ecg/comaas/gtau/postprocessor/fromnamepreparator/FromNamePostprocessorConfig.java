package com.ecg.comaas.gtau.postprocessor.fromnamepreparator;

import java.io.Serializable;

public class FromNamePostprocessorConfig implements Serializable {

    private static final long serialVersionUID = -8856707181092557219L;

    private final String buyerNameHeader;
    private final String sellerNameHeader;
    private final int order;

    public FromNamePostprocessorConfig(String buyerNameHeader, String sellerNameHeader, int order) {
        this.buyerNameHeader = buyerNameHeader;
        this.sellerNameHeader = sellerNameHeader;
        this.order = order;
    }

    public String getBuyerNameHeader() {
        return buyerNameHeader;
    }

    public String getSellerNameHeader() {
        return sellerNameHeader;
    }

    public int getOrder() {
        return order;
    }
}
