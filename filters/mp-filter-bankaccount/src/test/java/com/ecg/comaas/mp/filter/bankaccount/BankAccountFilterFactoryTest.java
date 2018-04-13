package com.ecg.comaas.mp.filter.bankaccount;

import com.ecg.comaas.mp.filter.bankaccount.BankAccountFilterConfiguration;
import com.ecg.comaas.mp.filter.bankaccount.BankAccountFilterFactory;
import com.ecg.comaas.mp.filter.bankaccount.DescriptionBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;

public class BankAccountFilterFactoryTest {
    @Mock
    DescriptionBuilder descriptionBuilder;

    @Test
    public void testParseConfiguration() throws Exception {
        BankAccountFilterFactory factory = new BankAccountFilterFactory(descriptionBuilder);

        String json = "{" +
                "    \"fraudulentBankAccounts\": [" +
                "        \"5819772\"," +
                "        \"5921956\"," +
                "        \"857382354\"," +
                "        \"3381614021\"," +
                "        \"6060108398\"," +
                "        \"BE77752340336455\"," +
                "        \"NL36RABO0160719446\"," +
                "        \"NL50INGB0063774032\"," +
                "        \"NL84RABO0118821586\"," +
                "        \"NL45SNSB0909111261\"," +
                "        \"NL27ABNA0440211618\"" +
                "    ]," +
                "    \"highCertaintyMatchScore\": 101," +
                "    \"lowCertaintyMatchScore\": \"41\"," +
                "    \"adIdMatchScore\": 21" +
                "}";

        JsonNode configurationJson = new ObjectMapper().readTree(json);

        BankAccountFilterConfiguration configuration = factory.parseConfiguration(configurationJson);

        assertEquals(101, configuration.getHighCertaintyMatchScore());
        assertEquals(41, configuration.getLowCertaintyMatchScore());
        assertEquals(21, configuration.getAdIdMatchScore());
    }

    @Test
    public void testParseConfigurationWithDefaults() throws Exception {
        BankAccountFilterFactory factory = new BankAccountFilterFactory(descriptionBuilder);

        String json = "{ \"fraudulentBankAccounts\": [ ]}";

        JsonNode configurationJson = new ObjectMapper().readTree(json);

        BankAccountFilterConfiguration configuration = factory.parseConfiguration(configurationJson);

        assertTrue(configuration.getFraudulentBankAccounts().isEmpty());
        assertEquals(100, configuration.getHighCertaintyMatchScore());
        assertEquals(50, configuration.getLowCertaintyMatchScore());
        assertEquals(20, configuration.getAdIdMatchScore());
    }
}
