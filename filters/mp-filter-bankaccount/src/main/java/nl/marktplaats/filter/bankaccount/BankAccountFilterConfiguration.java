package nl.marktplaats.filter.bankaccount;

import org.springframework.util.Assert;

import java.util.List;

class BankAccountFilterConfiguration {

    /**
     * Fraudulent bank account numbers in any format. E.g.:
     * - IBAN (NL84INGB0002930139)
     * - 6 or 7 digits (2930139)
     * - 9 or 10 digits (757706428)
     */
    private final List<String> fraudulentBankAccounts;

    /**
     * Score that will be used for a high certainty bank account match.
     */
    private final int highCertaintyMatchScore;

    /**
     * Score that will be used for bank account match of any other certainty.
     */
    private final int lowCertaintyMatchScore;

    /**
     * Score that will be used when the bank account match also matches the ad id of the mail.
     */
    private final int adIdMatchScore;

    public BankAccountFilterConfiguration(List<String> fraudulentBankAccounts) {
        this(fraudulentBankAccounts, 100, 50, 20);
    }

    public BankAccountFilterConfiguration(List<String> fraudulentBankAccounts, int highCertaintyMatchScore, int lowCertaintyMatchScore, int adIdMatchScore) {
        Assert.notNull(fraudulentBankAccounts);
        Assert.isTrue(
                highCertaintyMatchScore >= lowCertaintyMatchScore
                && lowCertaintyMatchScore >= adIdMatchScore
                && adIdMatchScore >= 0
        );
        this.fraudulentBankAccounts = fraudulentBankAccounts;
        this.highCertaintyMatchScore = highCertaintyMatchScore;
        this.lowCertaintyMatchScore = lowCertaintyMatchScore;
        this.adIdMatchScore = adIdMatchScore;
    }

    public List<String> getFraudulentBankAccounts() {
        return fraudulentBankAccounts;
    }

    public int getHighCertaintyMatchScore() {
        return highCertaintyMatchScore;
    }

    public int getLowCertaintyMatchScore() {
        return lowCertaintyMatchScore;
    }

    public int getAdIdMatchScore() {
        return adIdMatchScore;
    }

}
