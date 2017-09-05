package com.ecg.replyts.app.textcleanup;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EmailAddedTextExtractorTest {

    @Mock
    private Message message;

    @Mock
    private Message previousMessage;

    @Test
    public void newMessageResultsInEntireText() {
        when(message.getPlainTextBody()).thenReturn("message text");

        assertEquals("message text", EmailAddedTextExtractor.getNewText(message).in(Optional.empty()));
    }

    @Test
    public void testGetNewTextWithReplyMarker() throws Exception {
        when(message.getPlainTextBody()).thenReturn("message text new\n>old text, marked as reply");
        when(previousMessage.getPlainTextBody()).thenReturn("old text, marked as reply");

        assertEquals("message text new", EmailAddedTextExtractor.getNewText(message).in(Optional.of(previousMessage)));
    }

    @Test
    public void testGetNewTextSimple() throws Exception {
        when(message.getPlainTextBody()).thenReturn("message text new\nold text, marked as reply");
        when(previousMessage.getPlainTextBody()).thenReturn("old text, marked as reply");

        assertEquals("message text new", EmailAddedTextExtractor.getNewText(message).in(Optional.of(previousMessage)));
    }

    @Test
    public void testGetNewTextDoubledDiffs() throws Exception {
        when(message.getPlainTextBody()).thenReturn("message text new\nold text, marked as reply\nmessage text new");
        Message messageOld = mock(Message.class);
        when(messageOld.getPlainTextBody()).thenReturn("old text, marked as reply");

        assertEquals("message text new message text new", EmailAddedTextExtractor.getNewText(message).in(Optional.of(messageOld)));
    }

    @Test
    public void exposeCompletelyInsaneBug() {
        // this isa
        String firstMessage = "du penner\n" +
                "\n" +
                "Am 08.05.13 16:20, schrieb\n" +
                "interessent-25ws3lwop152l@mail.ebay-kleinanzeigen.de:\n" +
                "> eBay Kleinanzeigen | Kostenlos. Einfach. Lokal. Anzeigen gratis\n" +
                "> inserieren mit eBay Kleinanzeigen\n" +
                ">\n" +
                ">\n" +
                "> Anfrage zu Ihrer Kleinanzeige\n" +
                ">\n" +
                "> eBay Kleinanzeigen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Logo>\n" +
                ">\n" +
                ">\n" +
                "> Lieber Nutzer!\n" +
                ">\n" +
                "> Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige gesendet:\n" +
                "> 'HP Pavillion a7'\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/s-anzeige/hp-pavillion-a7/46545364-228-4560?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=ViewAd>\n" +
                ">\n" +
                "> Anzeigennummer: 46545364\n" +
                ">\n" +
                "> *Nachricht von:* test test\n" +
                ">\n" +
                "> western union für alle\n" +
                ">\n" +
                "> Beantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion\n" +
                "> Ihres E-Mail-Programms.\n" +
                ">\n" +
                "> Schützen Sie sich vor Betrug: Tipps für Ihre Sicherheit\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/sicherheitshinweise.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Tipps>\n" +
                ">\n" +
                ">\n" +
                "> Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken\n" +
                "> Sie bitte hier\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/spam-email-melden.html?raspd=6366dc54796ab19111b0ec71ab8f3d6cbf04b221b45ec409d9614effac142715614ede2a78cc1c78ec1ddf65f2cec64e9c88cf0987e38569edfe48e949de5fc0820a37aeb18e9bb3&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>.\n" +
                ">\n" +
                ">\n" +
                "> Zum Schutz unserer Nutzer filtern wir Spam und andere verdächtige\n" +
                "> Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf\n" +
                "> betrügerische Aktivitäten oder Verstößen gegen unsere\n" +
                "> Nutzungsbedingungen die Übermittlung von Nachrichten zu verzögern oder\n" +
                "> zu verweigern.\n" +
                ">\n" +
                "> Ihr eBay Kleinanzeigen-Team\n" +
                ">\n" +
                "> Wenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                "> oder\n" +
                "> kontaktieren Sie unseren Kundenservice.\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                ">\n" +
                ">\n" +
                "> Copyright © 2005-2013 eBay International AG. Alle Rechte vorbehalten.\n" +
                "> Ausgewiesene Marken gehören ihren jeweiligen Eigentümern.\n" +
                "> Hilfe\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Datenschutzerklärung\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/datenschutzerklaerung.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Nutzungsbedingungen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/nutzungsbedingungen.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Impressum\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/impressum.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Presse <http://presse.ebay.de> | Kontakt\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                ">\n" +
                ">";

        String secondMessage = "du penner\n" +
                "\n" +
                "Am 08.05.13 16:20, schrieb\n" +
                "interessent-25ws3lwop152l@mail.ebay-kleinanzeigen.de:\n" +
                "> eBay Kleinanzeigen | Kostenlos. Einfach. Lokal. Anzeigen gratis\n" +
                "> inserieren mit eBay Kleinanzeigen\n" +
                ">\n" +
                ">\n" +
                "> Anfrage zu Ihrer Kleinanzeige\n" +
                ">\n" +
                "> eBay Kleinanzeigen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Logo>\n" +
                ">\n" +
                ">\n" +
                "> Lieber Nutzer!\n" +
                ">\n" +
                "> Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige gesendet:\n" +
                "> 'HP Pavillion a7'\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/s-anzeige/hp-pavillion-a7/46545364-228-4560?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=ViewAd>\n" +
                ">\n" +
                "> Anzeigennummer: 46545364\n" +
                ">\n" +
                "> *Nachricht von:* test test\n" +
                ">\n" +
                "> western union für alle\n" +
                ">\n" +
                "> Beantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion\n" +
                "> Ihres E-Mail-Programms.\n" +
                ">\n" +
                "> Schützen Sie sich vor Betrug: Tipps für Ihre Sicherheit\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/sicherheitshinweise.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Tipps>\n" +
                ">\n" +
                ">\n" +
                "> Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken\n" +
                "> Sie bitte hier\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/spam-email-melden.html?raspd=6366dc54796ab19111b0ec71ab8f3d6cbf04b221b45ec409d9614effac142715614ede2a78cc1c78ec1ddf65f2cec64e9c88cf0987e38569edfe48e949de5fc0820a37aeb18e9bb3&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>.\n" +
                ">\n" +
                ">\n" +
                "> Zum Schutz unserer Nutzer filtern wir Spam und andere verdächtige\n" +
                "> Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf\n" +
                "> betrügerische Aktivitäten oder Verstößen gegen unsere\n" +
                "> Nutzungsbedingungen die Übermittlung von Nachrichten zu verzögern oder\n" +
                "> zu verweigern.\n" +
                ">\n" +
                "> Ihr eBay Kleinanzeigen-Team\n" +
                ">\n" +
                "> Wenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                "> oder\n" +
                "> kontaktieren Sie unseren Kundenservice.\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                ">\n" +
                ">\n" +
                "> Copyright © 2005-2013 eBay International AG. Alle Rechte vorbehalten.\n" +
                "> Ausgewiesene Marken gehören ihren jeweiligen Eigentümern.\n" +
                "> Hilfe\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Datenschutzerklärung\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/datenschutzerklaerung.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Nutzungsbedingungen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/nutzungsbedingungen.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Impressum\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/impressum.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n" +
                "> Presse <http://presse.ebay.de> | Kontakt\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n" +
                ">\n" +
                ">";


        when(message.getPlainTextBody()).thenReturn(secondMessage);

        when(previousMessage.getPlainTextBody()).thenReturn(firstMessage);

        String originalContent = EmailAddedTextExtractor.getNewText(message).in(Optional.of(previousMessage));


        // assertEquals("o", originalContent);
    }
}
