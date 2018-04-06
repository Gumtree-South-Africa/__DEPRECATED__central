package com.ecg.comaas.core.filter.activable;

public enum MailHeader {
    USER_TYPE("X-Process-Poster-Usertype"),
    /**
     * Comma-separated category ids path from root (excluding root itself).
     * For example: "27,174" represents the hierarchy of Level-1 category
     * "cars & vehicles" followed by the Level-2 category of "used cars &
     * trucks".
     */
    CATEGORY_PATH("X-Cust-Ad-Categories"),
    ;

    private String headerName;

    MailHeader(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }
}
