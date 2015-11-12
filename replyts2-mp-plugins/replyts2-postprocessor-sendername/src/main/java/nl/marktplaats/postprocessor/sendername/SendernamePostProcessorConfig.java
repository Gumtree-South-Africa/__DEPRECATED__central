package nl.marktplaats.postprocessor.sendername;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class SendernamePostProcessorConfig {
    private final String buyerNamePattern;
    private final String sellerNamePattern;

    @Autowired
    public SendernamePostProcessorConfig(@Value("${sendername.buyer.name.pattern}") String buyerNamePattern,
                                         @Value("${sendername.seller.name.pattern}") String sellerNamePattern) {
        this.buyerNamePattern = buyerNamePattern;
        this.sellerNamePattern = sellerNamePattern;
    }

    public String getBuyerNamePattern() {
        return buyerNamePattern;
    }

    public String getSellerNamePattern() {
        return sellerNamePattern;
    }

}
