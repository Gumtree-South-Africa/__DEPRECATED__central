package nl.marktplaats.filter.bankaccount;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by reweber on 19/10/15
 */
class BankAccountFilterConfiguration {

    /**
     * Fraudulent bank account numbers in any format. E.g.:
     * - IBAN (NL84INGB0002930139)
     * - 6 or 7 digits (2930139)
     * - 9 or 10 digits (757706428)
     */
    private List<String> fraudulentBankAccounts = new ArrayList<>();

    /**
     * Score that will be used for a high certainty bank account match.
     */
    private int highCertaintyMatchScore = 100;

    /**
     * Score that will be used for bank account match of any other certainty.
     */
    private int lowCertaintyMatchScore = 50;

    /**
     * Score that will be used when the bank account match also matches the ad id of the mail.
     */
    private int adIdMatchScore = 20;

    @Autowired
    public BankAccountFilterConfiguration(List<String> fraudulentBankAccounts) {
        this.fraudulentBankAccounts = fraudulentBankAccounts;
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

    public void setHighCertaintyMatchScore(int highCertaintyMatchScore) {
        this.highCertaintyMatchScore = highCertaintyMatchScore;
    }

    public void setLowCertaintyMatchScore(int lowCertaintyMatchScore) {
        this.lowCertaintyMatchScore = lowCertaintyMatchScore;
    }

    public void setAdIdMatchScore(int adIdMatchScore) {
        this.adIdMatchScore = adIdMatchScore;
    }
}
