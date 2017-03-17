package com.ecg.de.kleinanzeigen.replyts.buyeralias;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: acharton
 * Date: 11/12/13
 */
class BuyerAliasPostProcessor implements PostProcessor {

    private final String aliasFormatPattern;

    @Autowired
    BuyerAliasPostProcessor(String aliasFormatPattern) {
        this.aliasFormatPattern = aliasFormatPattern != null ? aliasFormatPattern : "%s";
    }

    @Override
    public int getOrder() {
        /* should run after cloaking the mail address */
        return Integer.MAX_VALUE;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        new BuyerAliasHandler(messageProcessingContext, aliasFormatPattern).handle();
    }
}
