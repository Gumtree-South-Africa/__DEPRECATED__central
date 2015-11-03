package nl.marktplaats.filter.bankaccount;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

/**
 * Created by reweber on 21/10/15
 */
public class BankAccountFinderTest {
    private BankAccountFilterConfiguration config;
    private BankAccountFinder finder;

    private static final String PLATFORM_AD_ID = "m975312468";

    @Before
    public void setUp() throws Exception {
        config = new BankAccountFilterConfiguration(asList(
                "123456",
                "942766011",
                "GB29NWBK60161331926819",
                "NL12INGP0000654321",
                "BE43068999999501",
                "DE05100205000003287300"));
        finder = new BankAccountFinder(config);
    }

    @Test public void findNothing() {
        String text = "Maak aub geld over aan rekening 1238456.";
        assertTrue(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID).isEmpty());
    }

    @Test public void findLiteralBankAccount() {
        String text = "Maak aub geld over aan rekening 123456.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItems(new BankAccountMatch("123456", "123456", 100)));
    }

    @Test public void findDottedBankAccount() {
        String text = "Maak aub geld over aan rekening 12.34.56.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("123456", "12.34.56", 100)));
    }

    @Test public void findSpacedBankAccount() {
        String text = "Maak aub geld over aan rekening 12 34 56.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("123456", "12 34 56", 100)));
    }

    @Test public void findDashedBankAccount() {
        String text = "Maak aub geld over aan rekening 12-34-56.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("123456", "12-34-56", 100)));
    }

    @Test public void findLowCertaintyLiteralBankAccount() {
        String text = "Maak aub geld over aan rekening 82123456.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("123456", "123456", 50)));
    }

    @Test public void findLowCertaintySpacedBankAccount() {
        String text = "Maak aub geld over aan rekening 12 34 56 94 3.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("123456", "12 34 56", 50)));
    }

    @Test public void findLiteralLocalBankAccountFromIban() {
        String text = "Maak aub geld over aan rekening 654321.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test public void findSpacedLocalBankAccountFromIban() {
        String text = "Maak aub geld over aan rekening 65 43 21.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("NL12INGP0000654321", "65 43 21", 100)));
    }

    @Test public void findLiteralIbanBankAccount() {
        String text = "Maak aub geld over aan rekening NL12INGP0000654321.";
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "NL12INGP0000654321", 100)));
        // Score for next one should maybe be 50.
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test public void findSpacedIbanBankAccount() {
        String text = "Maak aub geld over aan rekening NL12 INGP 0000 6543 21.";
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "NL12 INGP 0000 6543 21", 100)));
        // Score for next one should maybe be 50.
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "6543 21", 100)));
    }

    @Test public void findSpacedWrongCaseIbanBankAccount() {
        String text = "Maak aub geld over aan rekening nl12 ingp 0000 6543 21.";
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "nl12 ingp 0000 6543 21", 100)));
        // Score for next one should maybe be 50.
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "6543 21", 100)));
    }

    @Test public void findSpacedBelgiumIbanBankAccount() {
        String text = "Maak aub geld over aan rekening BE43 0689 9999 9501.";
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("BE43068999999501", "BE43 0689 9999 9501", 100)));
        assertThat(matches, hasItem(new BankAccountMatch("BE43068999999501", "689 9999 9501", 50)));
    }

    @Test public void findSpacedGermanIbanBankAccount() {
        String text = "Maak aub geld over aan rekening DE05 1002 0500 0003 2873 00.";
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("DE05100205000003287300", "DE05 1002 0500 0003 2873 00", 100)));
        assertThat(matches, hasItem(new BankAccountMatch("DE05100205000003287300", "3 2873 00", 50)));
    }

    @Test public void findLowCertaintyMatchOnOnlyLeadingZeros() {
        String text = "Maak aub geld over aan rekening 0000 0000 0000 0003 2873 00.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("DE05100205000003287300", "3 2873 00", 100)));
    }

    @Test public void findLowCertaintyMatchOnSerialNumber() {
        String text = "The ipod has serial number OBKKL0500 0003 2873 00.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("DE05100205000003287300", "3 2873 00", 50)));
    }

    @Test public void findLowCertaintyMatchOnSerialNumberWithTrailingNumbers() {
        String text = "Please use any of these bank accounts: 0003287300 00007653472.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("DE05100205000003287300", "3287300", 50)));
    }

    @Test public void findLowCertaintyMatchWithReallyALotOfSeparators() {
        String text = "Here is my bank account: 0     0     0     9     4     2     7     6     6     0     1     1     .";
        assertThat(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID), hasItem(new BankAccountMatch("942766011", "9     4     2     7     6     6     0     1     1", 50)));
    }

    @Test public void doNotFindWithCharactersInBankAccount() {
        String text = "Maak aub geld over aan rekening 123e456.";
        assertTrue(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID).isEmpty());
    }

    @Test public void doNotFindWithSpecialCharactersInBankAccount() {
        String text = "Maak aub geld over aan rekening 123é456.";
        assertTrue(finder.findBankAccountNumberMatches(asList(text), PLATFORM_AD_ID).isEmpty());
    }

    @Test public void findInMultipleTexts() {
        List<String> texts = asList("123456", "654321");
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(texts, PLATFORM_AD_ID);
        assertThat(matches.size(), is(2));
        assertThat(matches, hasItem(new BankAccountMatch("123456", "123456", 100)));
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test
    public void findInMultipleTextsHasNoDuplicates() {
        List<String> texts = asList("123456", "123456 or 654321");
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(texts, PLATFORM_AD_ID);
        assertThat(matches.size(), is(2));
        assertThat(matches, hasItem(new BankAccountMatch("123456", "123456", 100)));
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test
    public void containsSingleBankAccountNumber_FindsNothing() {
        List<String> texts = asList("ok", "innocent");
        List<BankAccountMatch> matches = finder.containsSingleBankAccountNumber("123456", texts, PLATFORM_AD_ID);
        assertThat(matches.size(), is(0));
    }

    @Test
    public void containsSingleBankAccountNumber_FindsLocalAccount() {
        List<String> texts = asList("ok", "suspect 123456");
        List<BankAccountMatch> matches = finder.containsSingleBankAccountNumber("123456", texts, PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("123456", "123456", 100)));
    }

    @Test
    public void containsSingleBankAccountNumber_FindsIbanAccount() {
        List<String> texts = asList("ok", "suspect 654321");
        List<BankAccountMatch> matches = finder.containsSingleBankAccountNumber("NL12INGP0000654321", texts, PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test
    public void containsSingleBankAccountNumber_doesNotFindOtherKnownAccounts() {
        List<String> texts = asList("ok", "other suspect: GB29NWBK60161331926819");
        List<BankAccountMatch> matches = finder.containsSingleBankAccountNumber("NL12INGP0000654321", texts, PLATFORM_AD_ID);
        assertThat(matches.size(), is(0));
    }

    @Test
    public void containsSingleBankAccountNumber_ignoresOtherKnownAccounts() {
        List<String> texts = asList("ok", "suspect 654321, other suspect: GB29NWBK60161331926819");
        List<BankAccountMatch> matches = finder.containsSingleBankAccountNumber("NL12INGP0000654321", texts, PLATFORM_AD_ID);
        assertThat(matches, hasItem(new BankAccountMatch("NL12INGP0000654321", "654321", 100)));
    }

    @Test
    public void findLiteralBankAccountSame_veryLowScoreForBankAccountSameAsAuroraAdId() {
        String text = "Maak aub geld over aan rekening 123456.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), "m123456"), hasItems(new BankAccountMatch("123456", "123456", 20)));
    }

    @Test
    public void findLiteralBankAccountSame_veryLowScoreForBankAccountSameAsPhpAdId() {
        String text = "Maak aub geld over aan rekening 123456.";
        assertThat(finder.findBankAccountNumberMatches(asList(text), "m123456"), hasItems(new BankAccountMatch("123456", "123456", 20)));
    }
}
