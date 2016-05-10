package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * Configuration Object for a bank account filter.
 */
class BankAccountFilterConfiguration {


    private final List<BankAccountConfiguration> bankConfigurations;

    public static class BankAccountConfiguration {

        private final String accountnumber;
        private final String bankCode;

        BankAccountConfiguration(String bankCode, String accountnumber) {
            this.bankCode = bankCode;
            this.accountnumber = accountnumber;
        }

        public String getAccountnumber() {
            return accountnumber;
        }

        public String getBankCode() {
            return bankCode;
        }
    }

    public BankAccountFilterConfiguration(List<BankAccountConfiguration> bankConfigurations) {
        this.bankConfigurations = bankConfigurations;
    }


    public List<BankAccountConfiguration> getFraudulentBankAccounts() {
        return bankConfigurations;
    }


    public static BankAccountFilterConfiguration from(JsonNode configuration) {
        ArrayNode rulesArray = (ArrayNode) configuration.get("rules");
        List<BankAccountConfiguration> configs = new ArrayList<BankAccountConfiguration>();
        for (JsonNode jsonNode : rulesArray) {
            String accountNumber = jsonNode.get("account").asText().replaceAll("[\\s-]", "");
            String bankCode = jsonNode.get("bankCode").asText().replaceAll("[\\s-]", "");
            new BigInteger(accountNumber);
            new BigInteger(bankCode);
            configs.add(new BankAccountConfiguration(bankCode, accountNumber));

        }

        return new BankAccountFilterConfiguration(configs);
    }
}
