package com.ecg.comaas.core.filter.bankaccount;

import com.ecg.comaas.core.filter.bankaccount.BankAccountFilterConfiguration;
import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BankAccountFilterConfigurationTest {


    @Test
    public void parsesJsonCorrectly() {
        String json = "{\n" +
                "    'rules': [{'account': '1234567890', 'bankCode': '987654321'}]\n" +
                " }";
        BankAccountFilterConfiguration config = BankAccountFilterConfiguration.from(JsonObjects.parse(json));


        assertEquals(1, config.getFraudulentBankAccounts().size());
        assertEquals("1234567890", config.getFraudulentBankAccounts().get(0).getAccountnumber());
        assertEquals("987654321", config.getFraudulentBankAccounts().get(0).getBankCode());
    }

    @Test
    public void removesSeperationCharactersFromConfig() {
        String json = "{\n" +
                "    'rules': [{'account': '123 456 78 90', 'bankCode': '987-654-321'}]\n" +
                " }";
        BankAccountFilterConfiguration config = BankAccountFilterConfiguration.from(JsonObjects.parse(json));


        assertEquals(1, config.getFraudulentBankAccounts().size());
        assertEquals("1234567890", config.getFraudulentBankAccounts().get(0).getAccountnumber());
        assertEquals("987654321", config.getFraudulentBankAccounts().get(0).getBankCode());
    }

    @Test(expected = NumberFormatException.class)
    public void rejectsNonNumericAccounts() {
        String json = "{\n" +
                "    'rules': [{'account': '123a', 'bankCode': '987-654-321'}]\n" +
                " }";
        BankAccountFilterConfiguration.from(JsonObjects.parse(json));
    }
}
