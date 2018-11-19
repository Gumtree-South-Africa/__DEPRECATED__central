package com.ecg.comaas.core.postprocessor.buyeralias;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;

@ComaasPlugin
@Profile({TENANT_GTUK, TENANT_EBAYK, TENANT_IT})
@Component
public class BuyerAliasPostProcessor implements PostProcessor {

    private final String aliasFormatPattern;

    @Autowired
    BuyerAliasPostProcessor(@Value("${replyts.buyeralias.formatPattern:%s über eBay Kleinanzeigen}") String aliasFormatPattern) {
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
