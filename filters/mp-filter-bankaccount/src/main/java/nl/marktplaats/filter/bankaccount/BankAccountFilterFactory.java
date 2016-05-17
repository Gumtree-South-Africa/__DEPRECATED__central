package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class BankAccountFilterFactory implements FilterFactory {

    private final DescriptionBuilder descriptionBuilder;

    @Autowired
    public BankAccountFilterFactory(DescriptionBuilder descriptionBuilder) {
        this.descriptionBuilder = descriptionBuilder;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {
        BankAccountFilterConfiguration bankAccountFilterConfiguration = parseConfiguration(configuration);
        BankAccountFinder bankAccountFinder = new BankAccountFinder(bankAccountFilterConfiguration);
        return new BankAccountFilter(bankAccountFinder, descriptionBuilder);
    }

    // default access for testing
    BankAccountFilterConfiguration parseConfiguration(JsonNode configuration) {
        // Example configuration (the bankaccount numbers are fake):
        //        {
        //            "fraudulentBankAccounts": [
        //                "5819772",
        //                "5921956",
        //                "857382354",
        //                "3381614021",
        //                "6060108398",
        //                "BE77752340336455",
        //                "NL36RABO0160719446",
        //                "NL50INGB0063774032",
        //                "NL84RABO0118821586",
        //                "NL45SNSB0909111261",
        //                "NL27ABNA0440211618"
        //            ],
        //            "highCertaintyMatchScore": 100,
        //            "lowCertaintyMatchScore": 40,
        //            "adIdMatchScore": 20
        //        }

        JsonNode fraudulentBankAccountsNode = configuration.get("fraudulentBankAccounts");
        List<String> fraudulentBankAccounts = new ArrayList<>(fraudulentBankAccountsNode.size());
        fraudulentBankAccountsNode
                .elements()
                .forEachRemaining(jsonNode -> fraudulentBankAccounts.add(jsonNode.asText()));

        int highCertaintyMatchScore = intValue(configuration, "highCertaintyMatchScore", 100);
        int lowCertaintyMatchScore = intValue(configuration, "lowCertaintyMatchScore", 50);
        int adIdMatchScore = intValue(configuration, "adIdMatchScore", 20);

        return new BankAccountFilterConfiguration(fraudulentBankAccounts, highCertaintyMatchScore, lowCertaintyMatchScore, adIdMatchScore);
    }

    private static int intValue(JsonNode configuration, String fieldName, int defaultValue) {
        JsonNode jsonNode = configuration.get(fieldName);
        if (jsonNode == null) return defaultValue;
        else if (jsonNode.isNumber()) return jsonNode.intValue();
        else return Integer.parseInt(jsonNode.asText());
    }
}
