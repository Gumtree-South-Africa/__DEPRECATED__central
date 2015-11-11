package nl.marktplaats.postprocessor.urlgateway.support;

import nl.marktplaats.postprocessor.urlgateway.UrlGatewayPostProcessorConfig;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Erik van Oosten
 */
public class BugPlainTextCustomerSupportLinkReplaced {

    private static final String MAIL_CONTENT =
            "\t\t\t\t\tU heeft een reactie van Piet Koper Marktplaats \n" +
            "            Op &lsquo;Genealogie en de Canon van Nederland&rsquo;\n" +
            "\t\t\t\n" +
                    "\t\t\n" +
            "\t\n" +
            "\t\t\t\t\tBeste Mark Justus,\n" +
            "\t\t\t\tIk wil kopen!\n" +
            "\t\t\t\t\t\t\t\t\t\t\tReageer direct\n" +
            "\t\t\t\t\tTips van Marktplaats \n" +
            "\t\t\t\t\t\t\t\tGebruik uw gezonde verstand: als iets te mooi lijkt om waar te zijn, dan is dat vaak ook zo.\n" +
            "\t\t\t\t\t\t\t\tWin informatie in over de koper en bewaar alle (verzend) informatie.\n" +
            "\t\t\t\t\t\t\t\tWees alert bij kopers uit het buitenland.\n" +
            "\t\t\t\t\n" +
            "\t\t\t\t\t Met vriendelijke groet, \n" +
            "\t\t\t\t\t\t\n" +
            "\t\t\t\t\t\tHet Marktplaats team\n" +
            "\t\t\t\t\t\twww.marktplaats.nl\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tAanbiedingen\n" +
            "\t\t\t\t\t\t\t\t\t\t\tDe leukste dagjes uit, goede restaurants, mooie producten en meer! Bekijk de Marktplaats Aanbieding van vandaag.\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tKlik hier\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tVeilig handelen\n" +
            "\t\t\t\t\t\t\t\t\t\t\tOm ervoor te zorgen dat u goed en veilig kunt handelen via Marktplaats, de belangrijkste tips op een rijtje: \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\thttp://statisch.marktplaats.nl/help/veilighandelen/\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tMarktplaats Manieren\n" +
            "\t\t\t\t\t\t\t\t\t\t\tVoorkom irritaties op Marktplaats.Marktplaats Manieren houdt het leuk!\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tLees meer\n" +
            "\t\t\t\t\t\t\t\t Aangeboden:  Genealogie en de Canon van Nederland\n" +
            "\t\t\t\t\t\t\t\t\t\t\tKenmerken\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t    Prijs: \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t    Bieden\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tHoogste bod:\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tRubriek:\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tOverige Boeken\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\thttp://link.marktplaats.nl/515496814\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\tGeplaatst:\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\t\t27-12-11\n" +
            "\t\t\t\t\t\t\t\tUw beschrijving\n" +
            "\t\t\t\t\t\t\t\tGenealogie en de canon. Deel I van 2. Jaarboek uitgave van het Centraal Bureau voor Genealogie 2008. Deel 62.\n" +
            "\t\t\t\t\t\t\t\n" +
            "\t\t\t\t\t\t\t\tUw contactgegevens\n" +
            "\t\t\t\t\t\t\t\t Mark JustusE-mail: mailto:mark.justus@gmail.com\n" +
            "\t\t\t\t\tContact&nbsp;&nbsp;|&nbsp;&nbsp;Mijn Marktplaats&nbsp;&nbsp;|&nbsp;&nbsp;Veilig handelen&nbsp;&nbsp;|&nbsp;&nbsp;Voorwaarden en Pricacybeleid\n" +
            "\t\t\t\t\tDit is een systeembericht. Vragen over deze email kunt u stellen via 'Contact'.\n" +
            "          Als u deze e-mail als http://marktplaats.custhelp.com/cgi-bin/marktplaats.cfg/php/enduser/std_adp.php?p_faqid=31 (ongewenste reclame) wilt afmelden, of als u deze \n" +
            "          e-mail niet vertrouwt, stuur dit bericht dan door naar spam@marktplaats.nl ";

    private GatewaySwitcher gatewaySwitcher;
    private PlainTextMailPartUrlGatewayRewriter underTest;

    @Before
    public void setUp() throws Exception {
        UrlGatewayPostProcessorConfig config = new UrlGatewayPostProcessorConfig();
        config.setGatewayUrl("http://gateway.marktplaats.nl/?url=[url]");
        config.setSkipDomains(asList("*.marktplaats.nl", "marktplaats.custhelp.com"));
        gatewaySwitcher = new GatewaySwitcher(config);
        underTest = new PlainTextMailPartUrlGatewayRewriter();
    }

    @Test
    public void testRewriteUrls_noRewrites() throws Exception {
        assertEquals(MAIL_CONTENT, underTest.rewriteUrls(MAIL_CONTENT, gatewaySwitcher));
    }

}
