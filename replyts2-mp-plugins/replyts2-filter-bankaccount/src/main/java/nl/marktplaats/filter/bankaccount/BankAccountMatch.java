package nl.marktplaats.filter.bankaccount;

import org.springframework.util.Assert;

/**
 * Created by reweber on 19/10/15
 */
public class BankAccountMatch {
    private String bankAccount;
    private String matchedText;
    private int score;

    public BankAccountMatch(String bankAccount, String matchedText, int score) {
        Assert.notNull(bankAccount);
        Assert.notNull(matchedText);
        this.bankAccount = bankAccount;
        this.matchedText = matchedText;
        this.score = score;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public int getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BankAccountMatch that = (BankAccountMatch) o;

        if (score != that.score) return false;
        if (!bankAccount.equals(that.bankAccount)) return false;
        if (!matchedText.equals(that.matchedText)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = bankAccount.hashCode();
        result = 31 * result + matchedText.hashCode();
        result = 31 * result + score;
        return result;
    }

    public BankAccountMatch withZeroScore() {
        return new BankAccountMatch(bankAccount, matchedText, 0);
    }

    @Override
    public String toString() {
        return "BankAccountMatch{" +
                "bankAccount='" + bankAccount + '\'' +
                ", matchedText='" + matchedText + '\'' +
                ", score=" + score +
                '}';
    }
}
