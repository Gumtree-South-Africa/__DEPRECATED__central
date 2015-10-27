package nl.marktplaats.filter.bankaccount;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

/**
 * Created by reweber on 21/10/15
 */
public class RealWorldBankAccountFinderTest {

    private static final String account1 = "716651333";
    private static final String message1 =
            "Hoi Tijmen, \n" +
                    "\n" +
                    "Inclusief verzendkosten komt het op € 12,50. Dit bedrag mag je overmaken op Raborekening 71.66.51.333 tnv C.Kooters te Arnhem ovv '4 cd's'. \n" +
                    "Zodra het bedrag op mijn rekening verschijnt stuur ik de cd's op. Vergeet niet om nog even je adres te mailen. \n" +
                    "\n" +
                    "Mvg, \n" +
                    "\n" +
                    "Bart\n";
    private static final String account2 = "619850779";
    private static final String message2 =
            "Hoi Gert, \n" +
                    "\n" +
                    "Ik kan het pakket zondag in de ochtend laten bezorgen. \n" +
                    "\n" +
                    "Mijn rekeningnummer is 619850779, Z. Asfalt, Amsterdam. 395 Euro was de afspraak. \n" +
                    "\n" +
                    "De MV komt compleet met adapter en muis. Je kunt ooit als je wil op eenvoudige wijze nog de hardeschijf vervangen door een grotere schijf. er zit nu 30 of 40 gb in. Er staan nog een aantal samplecds op de HD. \n" +
                    "Op youtube kun je heel wat filmpjes zien met tips en tricks. \n" +
                    "\n" +
                    "Groet, \n" +
                    "\n" +
                    "Wytse";
    private static final String account3 = "358211614";
    private static final String message3 =
            "Beste fawn, \n" +
                    "\n" +
                    "hiervoor is het overhemd voor jou! er komt wel 2,50 verzenkosten bij \n" +
                    "je mag het overmaken op raboreknr 3582 11614 tav FGH vd Eiken ovv blauw grijs overhemd met witte streep mt L. Waar mag ik het naartoe sturen? \n" +
                    "groetjes, \n" +
                    "An \n" +
                    "\n" +
                    "agora\n";
    private static final String account4 = "818416965";
    private static final String message4 =
            "Hallo Maurice, \n" +
                    "\n" +
                    "Als het verstuurd moet worden komt het totaal bedrag op 15,- \n" +
                    "Ik heb wel je adres nodig. \n" +
                    "\n" +
                    "K.S.L.Bitterhout \n" +
                    "\n" +
                    "reknr: 81.84.16.965 \n" +
                    "\n" +
                    "Mvg,Monique. \n" +
                    "\n" +
                    "Verstuurd vanaf mijn iPad";
    private static final String account5 = "830423194";
    private static final String message5 =
            "Geachte heer, mevrouw, \n" +
                    "Dank voor uw bieding, ik ga hiermee akkoord. U kunt de 5 euro plus 2.50 euro verzendkosten is 7.50 euro totaal overmaken op 830.423.194 ten name van Y.Spoor te Zeist. Graag dan nog uw adresgegevens. Groetjes schaapje@gmail.com \n" +
                    "\n" +
                    "Antieky";
    private static final String account6 = "1233114";
    private static final String message6 =
            "Beste @rena Zeephandel B.V., \n" +
                    "07-06-2012 \n" +
                    "Goedenavond, \n" +
                    "Bedankt voor uw bod van 1,25 voor Joegoslavie / Jugoslavie: mooi kavel van 25 verschillende zegels, gestempeld waarmee ik akkoord ben. \n" +
                    "De verzendkosten bedragen 0,50 \n" +
                    "Het totaalbedrag wordt dan 1,25+verzending 0,50 = Euro 1,75 \n" +
                    "Mijn gironummer is 1233114 \n" +
                    "(ten name van K.L. Fliet \n" +
                    "1827 AS Fluiten a/d Rijn) \n" +
                    "Graag hoor ik van u het adres waar ik de zegels naartoe kan sturen. \n" +
                    "Zodra het bedrag is bijgeschreven op mijn girorekening laat \n" +
                    "ik u dat meteen weten en zend ik de zegels, goed en veilig ingepakt \n" +
                    "tussen dun karton in een gewone envelop. \n" +
                    "Met vriendelijke groeten, \n" +
                    "Karel Fliet \n" +
                    "\n" +
                    "\n" +
                    "Ron D\n";
    private static final String account7 = "5250030";
    private static final String message7 =
            "Hallo Hanneke, \n" +
                    "\n" +
                    "Dat is prima. Als je dan € 10,00 overmaakt op mijn rek. 5250030 t.n.v. h.l.z. van Dik in Tilburg stuur ik je de tas toe. \n" +
                    "\n" +
                    "Mail je dan ook nog even je adres. \n" +
                    "\n" +
                    "gr. Fia\n";
    private static final String account8 = "5129453";
    private static final String message8 =
            "Dag Karin, \n" +
                    "\n" +
                    "Dat kan. Het opsturen kost €6,75. Dus in totaal wordt het dan €19,25 \n" +
                    "\n" +
                    "Als je hiermee akkoord gaat en je maakt dit bedrag over op onze rekening (5129453) en ons je adres doorgeeft, zullen wij de schoenen opsturen. \n" +
                    "\n" +
                    "Met vriendelijke groet, \n" +
                    "\n" +
                    "Marion de Haas\n";
    private static final String account9 = "9196719";
    private static final String message9 =
            "Hallo Joop, \n" +
                    "\n" +
                    "Als je 7,50 euro overmaakt op ING 9196719 \n" +
                    "dan komt cd cd jouw kant op. \n" +
                    "Kan cd trouwens pas dinsdag op de post doen (erg druk). \n" +
                    "\n" +
                    "Groet \n" +
                    "Ben Vrolijk \n" +
                    "Alkmaar\n";
    private static final String account10 = "2160841";
    private static final String message10 =
            "Geen probleem, \n" +
                    "\n" +
                    "je kunt het geld 20 euro en 6,75 verzendkosten overmaken tav Floortje Kann 2160841 ovv skeeler \n" +
                    "zodra ik het geld heb ontvangen verstuur ik de skeelers \n" +
                    "\n" +
                    "gr. debbie\n";

    private BankAccountFilterConfiguration config;
    private BankAccountFinder finder;

    private void assertFinds(String text, String bankAccountToDetect) {
        config = new BankAccountFilterConfiguration(Arrays.asList(bankAccountToDetect));
        finder = new BankAccountFinder(config);
        List<BankAccountMatch> matches = finder.findBankAccountNumberMatches(Arrays.asList(text), null);
        assertEquals(1, matches.size());
        assertThat(matches.get(0).getBankAccount(), is(bankAccountToDetect));
        assertThat(matches.get(0).getScore(), is(100));
    }

    @Test public void testMessage1() { assertFinds(message1, account1); }
    @Test public void testMessage2() { assertFinds(message2, account2); }
    @Test public void testMessage3() { assertFinds(message3, account3); }
    @Test public void testMessage4() { assertFinds(message4, account4); }
    @Test public void testMessage5() { assertFinds(message5, account5); }
    @Test public void testMessage6() { assertFinds(message6, account6); }
    @Test public void testMessage7() { assertFinds(message7, account7); }
    @Test public void testMessage8() { assertFinds(message8, account8); }
    @Test public void testMessage9() { assertFinds(message9, account9); }
    @Test public void testMessage10() { assertFinds(message10, account10); }
}
