package com.ecg.comaas.core.filter.bankaccount;

import com.ecg.comaas.core.filter.bankaccount.BankAccountFilterConfiguration.BankAccountConfiguration;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;

import java.util.List;

class BankAccountFilter implements Filter {
    private final BankAccountFilterConfiguration config;

    BankAccountFilter(BankAccountFilterConfiguration config) {
        this.config = config;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {

        for (String plaintextPart : context.getMail().get().getPlaintextParts()) {
            NumberStream numberStream = new NumberStreamExtractor(10, plaintextPart).extractStream();

            for (BankAccountConfiguration account : config.getFraudulentBankAccounts()) {
                if(numberStream.containsBoth(account.getAccountnumber(), account.getBankCode())) {

                    String foundAccount = account.getAccountnumber()+"/"+account.getBankCode();

                    return Lists.newArrayList(new FilterFeedback(foundAccount, "Contains blocked bank account "+foundAccount, 0, FilterResultState.HELD));
                }
            }

        }

        return null;
    }
}
