package ca.kijiji.replyts;

import java.util.Optional;

public enum BoxHeaders {
    POSTER_NAME("X-Process-Poster-Name"),
    REPLIER_NAME("X-Process-Replier-Name"),
    REPLIER_ID("X-Process-Replier-Userid"),
    SENDER_IP_ADDRESS("X-Cust-Ip-Address"),
    LOCALE("X-Process-Locale"),
    POSTER_ID("X-Process-Poster-Userid"),
    HTTP_HEADER_ID("X-Process-Http-Headerid"),
    HAS_ATTACHMENT("X-Process-Has-Attachment"),
    ANONYMIZE("X-Cust-Anonymize"), // x-cust, so we could search in ES and convo context
    MESSAGE_ORIGIN("X-Process-Origin")
    ;

    private String headerName;
    private Optional<String> customConversationValueName;

    BoxHeaders(String headerName) {
        this.headerName = headerName;

        if (!headerName.startsWith("X-Cust")) {
            this.customConversationValueName = Optional.empty();
        } else {
            this.customConversationValueName = Optional.of(headerName.substring(headerName.indexOf("X-Cust-") + 7).toLowerCase());
        }
    }

    public String getHeaderName() {
        return headerName;
    }

    /**
     * @return If the header is a Conversation Custom Value setter,
     * returns the custom value name (without the X-Cust prefix). Otherwise, returns empty.
     */
    public Optional<String> getCustomConversationValueName() {
        return customConversationValueName;
    }
}
