package nl.marktplaats.filter.bankaccount;

import org.apache.commons.lang.StringUtils;
import org.springframework.util.Assert;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

public class BankAccountFinder {
    private static final String LATIN1_SUPPLEMENT_2ndHALF = "\u00C0-\u00FF";
    private static final String LATIN_EXTENDED_A = "\u0100-\u017F";
    private static final String LATIN_EXTENDED_B = "\u0180-\u024F";
    private static final String IGNORABLE_CHARACTERS =
            "a-zA-Z" + LATIN1_SUPPLEMENT_2ndHALF + LATIN_EXTENDED_A + LATIN_EXTENDED_B;
    private static final Pattern IGNORABLE_CHARACTERS_PATTERN = Pattern.compile("[" + IGNORABLE_CHARACTERS + "]");

    private static final String IGNORABLE_SEPARATORS = "[^0-9" + IGNORABLE_CHARACTERS + "]{0,5}";
    private static final String IGNORABLE_SEPARATORS_WITH_0 = "[^1-9" + IGNORABLE_CHARACTERS + "]{0,30}";

    private static final Pattern DIGITS_AFTER_PATTERN = Pattern.compile("^" + IGNORABLE_SEPARATORS + "[0-9]");
    private static final Pattern DIGITS_BEFORE_PATTERN = Pattern.compile("[1-9]" + IGNORABLE_SEPARATORS_WITH_0 + "$");

    private BankAccountFilterConfiguration config;
    private List<AccountPattern> accountPatterns;

    public BankAccountFinder(BankAccountFilterConfiguration config) {
        this.config = config;
        accountPatterns = createPatterns(config.getFraudulentBankAccounts());
    }

    public List<BankAccountMatch> findBankAccountNumberMatches(List<String> texts, String adId) {
        return findMatchesForPatterns(texts, accountPatterns, adId);
    }

    public List<BankAccountMatch> containsSingleBankAccountNumber(String bankAccountNumber, List<String> texts, String adId) {
        List<AccountPattern> patternsForBan = new ArrayList<AccountPattern>(2);
        for (AccountPattern accountPattern : accountPatterns) {
            if (accountPattern.bankAccount.equals(bankAccountNumber)) patternsForBan.add(accountPattern);
        }
        return findMatchesForPatterns(texts, patternsForBan, adId);
    }

    private List<BankAccountMatch> findMatchesForPatterns(List<String> texts, List<AccountPattern> patterns, String adId) {
        String cleanedAdId = cleanAdId(adId);
        Iterable<String> digitStrings = extractDigitStrings(texts);
        List<AccountPattern> presentPatterns = presentAccountPatterns(digitStrings, patterns);
        List<BankAccountMatch> matches = new ArrayList<BankAccountMatch>();
        for (String text : texts) {
            Iterable<String> lines = linesWithDigits(text);
            for (String line : lines) {
                for (AccountPattern accountPattern : presentPatterns) {
                    boolean adIdSameAsBankAccountNumber = accountPattern.bankAccount.equals(cleanedAdId);
                    addMatchesForPattern(matches, line, accountPattern, adIdSameAsBankAccountNumber);
                }
            }
        }
        return matches;
    }

    private String cleanAdId(String rawPlatformAdId) {
        return rawPlatformAdId == null ? null : rawPlatformAdId.replaceAll("[^0-9]", "");
    }

    private List<AccountPattern> presentAccountPatterns(Iterable<String> digitStrings, List<AccountPattern> patterns) {
        List<AccountPattern> presentAccountPatterns = new ArrayList<AccountPattern>();
        for (String digitString : digitStrings) {
            for (AccountPattern accountPattern : patterns) {
                if (digitString.contains(accountPattern.digitsOnly)) presentAccountPatterns.add(accountPattern);
            }
        }
        return presentAccountPatterns;
    }

    private void addMatchesForPattern(List<BankAccountMatch> matches, String line, AccountPattern accountPattern, boolean adIdSameAsBankAccountNumber) {
        if (accountPattern.ibanRePattern != null) {
            addMatchesForRePattern(matches, line, accountPattern.ibanRePattern, accountPattern.bankAccount, adIdSameAsBankAccountNumber);
        }
        if (accountPattern.localRePattern != null) {
            addMatchesForRePattern(matches, line, accountPattern.localRePattern, accountPattern.bankAccount, adIdSameAsBankAccountNumber);
        }
    }

    private void addMatchesForRePattern(List<BankAccountMatch> matches, String line, Pattern rePattern, String bankAccount, boolean adIdSameAsBankAccountNumber) {
        Matcher m = rePattern.matcher(line);
        while (m.find()) {
            int score = getScore(line, m, adIdSameAsBankAccountNumber);
            BankAccountMatch match = new BankAccountMatch(
                    bankAccount,
                    m.group(),
                    score);
            if (!matches.contains(match)) matches.add(match);
        }
    }

    private int getScore(String line, Matcher m, boolean adIdSameAsBankAccountNumber) {
        if (m.group().length() > 40)
            // Many many separators
            return config.getLowCertaintyMatchScore();

        else {
            String before = line.substring(0, m.start());
            String after = line.substring(m.end());

            if (DIGITS_BEFORE_PATTERN.matcher(before).find() || DIGITS_AFTER_PATTERN.matcher(after).find()) {
                return config.getLowCertaintyMatchScore();
            } else {
                // No prefixes, no postfixes
                if (adIdSameAsBankAccountNumber) {
                    return config.getAdIdMatchScore();
                } else {
                    return config.getHighCertaintyMatchScore();
                }
            }
        }
    }

    private Iterable<String> linesWithDigits(final String text) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private int textIndex = 0;
                    private String nextLine;
                    private boolean advanceNextCall = true;

                    @Override
                    public boolean hasNext() {
                        if (advanceNextCall) findNextLineWithDigit();
                        advanceNextCall = false;
                        return nextLine != null;
                    }

                    @Override
                    public String next() {
                        if (advanceNextCall) findNextLineWithDigit();
                        advanceNextCall = true;
                        if (nextLine == null) throw new NoSuchElementException();
                        return nextLine;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void findNextLineWithDigit() {
                        nextLine = null;
                        boolean foundDigitInCurrentLine = false;
                        int beginIndex = textIndex;
                        while (textIndex < text.length()) {
                            char c = text.charAt(textIndex);
                            ++textIndex;
                            foundDigitInCurrentLine = foundDigitInCurrentLine || isAsciiDigit(c);
                            if (isNewLine(c)) {
                                if (foundDigitInCurrentLine) {
                                    nextLine = text.substring(beginIndex, textIndex);
                                    return;
                                } else {
                                    beginIndex = textIndex;
                                }
                            }
                        }
                        if (foundDigitInCurrentLine) {
                            nextLine = text.substring(beginIndex, textIndex);
                        }
                    }
                };
            }
        };
    }

    private List<AccountPattern> createPatterns(List<String> fraudulentBankAccounts) {
        List<AccountPattern> patterns = new ArrayList<AccountPattern>(fraudulentBankAccounts.size());
        for (String account : fraudulentBankAccounts) {
            patterns.add(createPattern(account));
        }
        return Collections.unmodifiableList(patterns);
    }

    private AccountPattern createPattern(String account) {
        if (isIban(account)) {
            return createIbanPattern(account);
        } else {
            return createLocalPattern(account);
        }
    }

    private boolean isIban(String account) {
        return account.matches("[A-Z][A-Z][0-9][0-9].+");
    }

    private AccountPattern createIbanPattern(String ibanAccount) {
        String localAccount = extractLocalAccountFrom(ibanAccount);
        if (localAccount != null) {
            // An IBAN from a known country
            return new AccountPattern(
                    ibanAccount, localAccount,
                    regularExpressionForAccount(localAccount), regularExpressionForAccount(ibanAccount));
        } else {
            // An IBAN from an unknown country
            String digitsInIban = ibanAccount.replaceAll("[^0-9]", "");
            return new AccountPattern(
                    ibanAccount, digitsInIban,
                    null, regularExpressionForAccount(ibanAccount));
        }
    }

    private String extractLocalAccountFrom(String ibanAccount) {
        if (ibanAccount.startsWith("NL")) {
            if (ibanAccount.length() > 8) {
                return dropLeadingZero(ibanAccount.substring(8));
            }
        }
        if (ibanAccount.startsWith("BE")) {
            if (ibanAccount.length() > 4) {
                return dropLeadingZero(ibanAccount.substring(4));
            }
        }
        if (ibanAccount.startsWith("DE")) {
            if (ibanAccount.length() > 12) {
                return dropLeadingZero(ibanAccount.substring(12));
            }
        }
        return null;
    }

    private String dropLeadingZero(String substring) {
        return StringUtils.stripStart(substring, "0");
    }

    private AccountPattern createLocalPattern(String localAccount) {
        return new AccountPattern(localAccount, localAccount, regularExpressionForAccount(localAccount), null);
    }

    private Pattern regularExpressionForAccount(String account) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < account.length(); i++) {
            char c = account.charAt(i);
            if (i != 0) {
                sb.append(IGNORABLE_SEPARATORS);
            }
            if (isAsciiDigit(c)) {
                sb.append(c);
            } else {
                sb.append('[').append(toUpperCase(c)).append(toLowerCase(c)).append(']');
            }
        }
        String re = sb.toString();
        return Pattern.compile(re);
    }

    private Iterable<String> extractDigitStrings(final List<String> texts) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private Iterator<String> textIterator = texts.iterator();
                    private String text;
                    private int textIndex = 0;
                    private String nextDigitString;
                    private boolean advanceNextCall = true;

                    @Override
                    public boolean hasNext() {
                        if (advanceNextCall) findNextDigitString();
                        advanceNextCall = false;
                        return nextDigitString != null;
                    }

                    @Override
                    public String next() {
                        if (advanceNextCall) findNextDigitString();
                        advanceNextCall = true;
                        if (nextDigitString == null) throw new NoSuchElementException();
                        return nextDigitString;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void findNextDigitString() {
                        StringBuilder sb = new StringBuilder(40);
                        setOrAdvanceCurrentText();
                        nextDigitString = null;
                        while (text != null) {
                            while (textIndex < text.length()) {
                                char c = text.charAt(textIndex);
                                ++textIndex;
                                if (isAsciiDigit(c)) {
                                    sb.append(c);
                                } else if ((isLetter(c) || isNewLine(c)) && sb.length() != 0) {
                                    nextDigitString = sb.toString();
                                    return;
                                }
                            }
                            if (sb.length() > 0) {
                                nextDigitString = sb.toString();
                                return;
                            } else {
                                setOrAdvanceCurrentText();
                            }
                        }
                    }

                    private void setOrAdvanceCurrentText() {
                        if (text == null || textIndex == text.length()) {
                            textIndex = 0;
                            text = textIterator.hasNext() ? textIterator.next() : null;
                        }
                    }
                };
            }
        };
    }

    private boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return IGNORABLE_CHARACTERS_PATTERN.matcher(String.valueOf(c)).matches();
    }

    private boolean isNewLine(char c) {
        return c == '\n' || c == '\r';
    }

    private static class AccountPattern {
        public final String bankAccount;
        public final String digitsOnly;
        public final Pattern localRePattern;
        public final Pattern ibanRePattern;

        private AccountPattern(String bankAccount, String digitsOnly, Pattern localRePattern, Pattern ibanRePattern) {
            Assert.notNull(bankAccount);
            Assert.notNull(digitsOnly);
            this.bankAccount = bankAccount;
            this.digitsOnly = digitsOnly;
            this.localRePattern = localRePattern;
            this.ibanRePattern = ibanRePattern;
        }
    }
}
