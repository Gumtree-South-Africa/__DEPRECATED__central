package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by reweber on 20/10/15
 */
public class BankAccountFilterFactory implements FilterFactory {

    //TODO check if the key matches the key in the config
    private static final String FRAUDULENT_BANK_ACCOUNTS_KEY = "fraudulentBankAccounts";

    private final MailCloakingService mailCloakingService;
    private final MailRepository mailRepository;

    @Autowired
    public BankAccountFilterFactory(MailCloakingService mailCloakingService, MailRepository mailRepository) {
        this.mailCloakingService = mailCloakingService;
        this.mailRepository = mailRepository;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configuration) {

        List<String> fraudulentBankAccounts = Lists.newArrayList(configuration.get(FRAUDULENT_BANK_ACCOUNTS_KEY).elements()).stream()
                .map(JsonNode::asText).collect(Collectors.toList());

        BankAccountFilterConfiguration bankAccountFilterConfiguration = new BankAccountFilterConfiguration(fraudulentBankAccounts);
        BankAccountFinder bankAccountFinder = new BankAccountFinder(bankAccountFilterConfiguration);
        return new BankAccountFilter(bankAccountFinder, mailCloakingService, mailRepository, new Mails());
    }
}
