package com.ecg.comaas.gtuk.postprocessor.bodyanonymizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MessageBodyAnonymizerConfig {
    private final String sellerKnownGoodHeader = "X-Cust-Sellergood";
    private final String sellerKnownGoodValue = "ACCOUNT_HOLDER";

    private final String revealEmailHeader = "X-Cust-Emailaddresscloak";
    private final String revealEmailValue = "REVEAL";

    private String knownGoodInsertFooterFormat;

    private String safetyTextFormat;
    private String knownGoodSellerSafetyTextFormat;

    @Autowired
    public MessageBodyAnonymizerConfig(
            @Value("gumtree.anonymizer.knowngoodinsertfooter") String knownGoodInsertFooterFormat,
            @Value("gumtree.anonymizer.safetytext") String safetyTextFormat,
            @Value("gumtree.anonymizer.knowngoodsellersafetytext") String knownGoodSellerSafetyTextFormat) {
        this.knownGoodInsertFooterFormat = knownGoodInsertFooterFormat;
        this.safetyTextFormat = safetyTextFormat;
        this.knownGoodSellerSafetyTextFormat = knownGoodSellerSafetyTextFormat;
    }

    public String getSellerKnownGoodHeader() {
        return sellerKnownGoodHeader;
    }

    public String getSellerKnownGoodValue() {
        return sellerKnownGoodValue;
    }

    public String getKnownGoodInsertFooterFormat() {
        return knownGoodInsertFooterFormat;
    }

    public String getSafetyTextFormat() {
        return safetyTextFormat;
    }

    public String getKnownGoodSellerSafetyTextFormat() {
        return knownGoodSellerSafetyTextFormat;
    }

    public String getRevealEmailHeader() {
        return revealEmailHeader;
    }

    public String getRevealEmailValue() {
        return revealEmailValue;
    }
}
