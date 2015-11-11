package nl.marktplaats.postprocessor.sendername;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendernamePostProcessorConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SendernamePostProcessorConfig.class);
    private String buyerConversationHeader;
    private String sellerConversationHeader;
    private String buyerNamePattern;
    private String sellerNamePattern;
    private boolean fallbackToConversationId;

    public SendernamePostProcessorConfig() {

    }

    public SendernamePostProcessorConfig(String buyerConversationHeader, String sellerConversationHeader, String buyerNamePattern, String sellerNamePattern, boolean fallbackToConversationId) {
        this.buyerConversationHeader = buyerConversationHeader;
        this.sellerConversationHeader = sellerConversationHeader;
        this.buyerNamePattern = buyerNamePattern;
        this.sellerNamePattern = sellerNamePattern;
        this.fallbackToConversationId = fallbackToConversationId;
    }

    public String getBuyerConversationHeader() {
        return buyerConversationHeader;
    }

    public void setBuyerConversationHeader(String buyerConversationHeader) {
        this.buyerConversationHeader = buyerConversationHeader;
    }

    public String getBuyerNamePattern() {
        return buyerNamePattern;
    }

    public void setBuyerNamePattern(String buyerNamePattern) {
        this.buyerNamePattern = buyerNamePattern;
    }

    public String getSellerConversationHeader() {
        return sellerConversationHeader;
    }

    public void setSellerConversationHeader(String sellerConversationHeader) {
        this.sellerConversationHeader = sellerConversationHeader;
    }

    public String getSellerNamePattern() {
        return sellerNamePattern;
    }

    public void setSellerNamePattern(String sellerNamePattern) {
        this.sellerNamePattern = sellerNamePattern;
    }

    public boolean isFallbackToConversationId() {
        return fallbackToConversationId;
    }

    public void setFallbackToConversationId(boolean fallbackToConversationId) {
        this.fallbackToConversationId = fallbackToConversationId;
    }

}
