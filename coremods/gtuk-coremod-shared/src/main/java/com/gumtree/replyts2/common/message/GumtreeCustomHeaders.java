package com.gumtree.replyts2.common.message;

public enum GumtreeCustomHeaders {
    CATEGORY("X-Cust-Categoryid"),
    BUYER_COOKIE("X-Cust-Buyercookie"),
    BUYER_IP("X-Cust-Buyerip"),
    SELLER_GOOD("X-Cust-Sellergood"),
    BUYER_GOOD("X-Cust-Buyergood"),
    SELLER_IS_PRO("X-Cust-Sellerispro"),
    BUYER_IS_PRO("X-Cust-Buyerispro"),
    SELLER_ID("X-Cust-Sellerid"),
    BUYER_ID("X-Cust-Buyerid"),
    CLIENT_ID("X-Cust-Clientid");

    String headerValue;

    private GumtreeCustomHeaders(String headerValue) {
        this.headerValue = headerValue;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
