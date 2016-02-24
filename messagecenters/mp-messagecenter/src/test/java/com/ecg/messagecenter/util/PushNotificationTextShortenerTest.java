package com.ecg.messagecenter.util;

import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: maldana
 * Date: 28.10.13
 * Time: 10:22
 *
 * @author maldana@ebay.de
 */
public class PushNotificationTextShortenerTest {

    public static final String FIRST_TEXT = "eBay Kleinanzeigen | Kostenlos. Einfach. Lokal. Anzeigen gratis inserieren mit eBay Kleinanzeigen\n\n Anfrage zu Ihrer Kleinanzeige\n\nLieber Nutzer! \n\n Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige gesendet: \n 'Runder Tisch, Bugholz-Stil' \n Anzeigennummer: 113817521\n\nNachricht von: Clara \n\n Hallo, \n \nbin an den Tisch interessiert.\n\nBeantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion Ihres E-Mail-Programms.\n\nSchützen Sie sich vor Betrug: Tipps f<C3><BC>r Ihre Sicherheit \n\n Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken Sie bitte hier.\n\n Zum Schutz unserer Nutzer filtern wir Spam und andere verd<C3><A4>chtige Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf betr<C3><BC>gerische Aktivit<C3><A4>ten oder Verst<C3><B6><C3><9F>en gegen unsere Nutzungsbedingungen die <C3><9C>bermittlung von Nachrichten zu verz<C3><B6>gern oder zu verweigern.\n\nIhr eBay Kleinanzeigen-Team\n\nWenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten oder\n kontaktieren Sie unseren Kundenservice.\n\nCopyright <C2><A9> 2005-2013 eBay International AG. Alle Rechte vorbehalten.\n Ausgewiesene Marken geh<C3><B6>ren ihren jeweiligen Eigent<C3><BC>mern. \n\n Hilfe | Datenschutzerkl<C3><A4>rung | Nutzungsbedingungen | Impressum | Presse | Kontakt";
    public static final String LATER_ANSWERS_TEXT = "Ist noch da :) Abholung heute abend ab 20 uhr (Gosse.16, fhain)?\n\n On 05/17/2013 01:28 PM, \ninteressent-10ziewgzf03a4@mail.ebay-kleinanzeigen.de wrote:\n>\n>   Anfrage zu Ihrer Kleinanzeige\n>\n> \\teBay Kleinanzeigen\n> <http://kleinanzeigen.ebay.de/anzeigen/?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Logo>\n>\n>\n> Lieber Nutzer!\n>\n> Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige gesendet:\n> 'Runder Tisch, Bugholz-Stil'\n> <http://kleinanzeigen.ebay.de/anzeigen/s-anzeige/runder-tisch,-bugholz-stil/113817521-88-3352?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=ViewAd>\n>\n> Anzeigennummer: 113817521\n>\n> *Nachricht von:* Clara\n>\n> Hallo,\n>\n> bin an den Tisch interessiert.\n>\n> Beantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion\n> Ihres E-Mail-Programms.\n>\n> Sch<C3><BC>tzen Sie sich vor Betrug: Tipps f<C3><BC>r Ihre Sicherheit\n> <http://kleinanzeigen.ebay.de/anzeigen/sicherheitshinweise.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Tipps>\n>\n>\n> Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken\n> Sie bitte hier\n> <http://kleinanzeigen.ebay.de/anzeigen/spam-email-melden.html?raspd=cc91724cb37644daea0ca9cae51e4d1577ddb22d3edc230f8a070279fb459c6514d02d42ccdd6677c7a29fca7978ecc51331c440ab1fabd55410b0e10680ec9a7919165094ef88496b19eb30cc778efceb928f76c7fc1f65a35cdd5485ce2f10&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>.\n>\n>\n> Zum Schutz unserer Nutzer filtern wir Spam und andere verdächtige\n> Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf\n> betrügerische Aktiviäten oder Verstößen\n" +
            "ßen gegen unsere\n> Nutzungsbedingungen die Übermittlung von Nachrichten zu verzögern oder\n> zu verweigern.\n>\n> Ihr eBay Kleinanzeigen-Team\n>\n> Wenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten\n> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n> oder\n> kontaktieren Sie unseren Kundenservice.\n> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n>\n>\n> Copyright <C2><A9> 2005-2013 eBay International AG. Alle Rechte vorbehalten.\n> Ausgewiesene Marken gehören ihren jeweiligen Eigentümern.\n> Hilfe\n> <http://kleinanzeigen.ebay.de/anzeigen/hilfe.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n> Datenschutzerkl<C3><A4>rung\n> <http://kleinanzeigen.ebay.de/anzeigen/datenschutzerklaerung.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n> Nutzungsbedingungen\n> <http://kleinanzeigen.ebay.de/anzeigen/nutzungsbedingungen.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n> Impressum\n> <http://kleinanzeigen.ebay.de/anzeigen/impressum.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other> |\n> Presse <http://presse.ebay.de> | Kontakt\n> <http://kleinanzeigen.ebay.de/anzeigen/kontakt.html?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>\n>\n>\n\n\n";

    public static final String MESSAGEBOX_HAGGLING_REPLY_TEXT = "und noch ein angebot! \n" +
            "Angebot: 234,00 EUR\n" +
            "Angebot annehmen \n" +
            "Gegenangebot";

    public static final String MESSAGEBOX_HAGGLING_CP_TEXT = "eBay Kleinanzeigen | Kostenlos. Einfach. Lokal. Anzeigen gratis inserieren mit eBay Kleinanzeigen\n" +
            "\n" +
            " Anfrage zu Ihrer Anzeige\n" +
            "\n" +
            "Lieber Nutzer! \n" +
            "\n" +
            " Ein Interessent hat eine Anfrage zu Ihrer Anzeige gesendet: \n" +
            " 'Category has no random titles' \n" +
            " Anzeigennummer: 207754130\n" +
            "\n" +
            "Nachricht von: Michael Lommatzsch \n" +
            "\n" +
            "Hello\n" +
            "\n" +
            " Angebot: 213,00 EUR\n" +
            "\n" +
            " Angebot annehmen Gegenangebot\n" +
            "\n" +
            "Beantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion Ihres E-Mail-Programms.\n" +
            "\n" +
            "Schützen Sie sich vor Betrug: Tipps für Ihre Sicherheit \n" +
            "\n" +
            " Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken Sie bitte jetzt melden.\n" +
            "\n" +
            " Zum Schutz unserer Nutzer filtern wir Spam und andere verdächtige Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf betrügerische Aktivitäten oder Verstößen gegen unsere Nutzungsbedingungen die Übermittlung von Nachrichten zu verzögern oder zu verweigern.\n" +
            "\n" +
            "Ihr eBay Kleinanzeigen-Team\n" +
            "\n" +
            "Wenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten oder\n" +
            " kontaktieren Sie unseren Kundenservice.\n" +
            "\n" +
            "Copyright © 2005-2014 eBay International AG. Alle Rechte vorbehalten.\n" +
            " Ausgewiesene Marken gehören ihren jeweiligen Eigentümern. \n" +
            "\n" +
            " Hilfe | Datenschutzerklärung | Nutzungsbedingungen | Impressum | Presse | Kontakt";


    public static final String LATER_ANSWERS_TEXT_2="eBay Kleinanzeigen | Kostenlos. Einfach. Lokal. Anzeigen gratis inserieren mit eBay Kleinanzeigen\n" +
            "\n" +
            " Anfrage zu Ihrer Kleinanzeige\n" +
            "\n" +
            "Lieber Nutzer! \n" +
            "\n" +
            " Ein Interessent hat eine Anfrage zu Ihrer Kleinanzeige gesendet: \n" +
            " 'Fahrradsitz - Römer Jockey Comfort' \n" +
            " Anzeigennummer: 145767436\n" +
            "\n" +
            "Nachricht von: manuel \n" +
            "\n" +
            " hallo, \n" +
            "\n" +
            "ist die Halterung auch mit dabei? Ist aus den Bildern und Text nicht ersichtlich.\n" +
            "\n" +
            "Danke für die Info.\n" +
            "\n" +
            "Artikel bereits verkauft? Löschen Sie Ihre Anzeige und informieren alle Interessenten, dass der Artikel bereits verkauft ist. \n" +
            "\n" +
            " Schon verkauft? Anzeige löschen Anzeige deaktivieren\n" +
            "\n" +
            "Beantworten Sie diese Nachricht einfach mit der 'Antworten'-Funktion Ihres E-Mail-Programms.\n" +
            "\n" +
            "Schützen Sie sich vor Betrug: Tipps für Ihre Sicherheit \n" +
            "\n" +
            " Falls Sie uns diese E-Mail als Spam oder Betrug melden wollen, klicken Sie bitte hier.\n" +
            "\n" +
            " Zum Schutz unserer Nutzer filtern wir Spam und andere verdächtige Nachrichten. Wir behalten uns vor, bei konkretem Verdacht auf betrügerische Aktivitäten oder Verstößen gegen unsere Nutzungsbedingungen die Übermittlung von Nachrichten zu verzögern oder zu verweigern.\n" +
            "\n" +
            "Ihr eBay Kleinanzeigen-Team\n" +
            "\n" +
            "Wenn Sie Fragen haben, schauen Sie bitte auf unsere Hilfeseiten oder\n" +
            " kontaktieren Sie unseren Kundenservice.\n" +
            "\n" +
            "Copyright © 2005-2013 eBay International AG. Alle Rechte vorbehalten.\n" +
            " Ausgewiesene Marken gehören ihren jeweiligen Eigentümern. \n" +
            "\n" +
            " Hilfe | Datenschutzerklärung | Nutzungsbedingungen | Impressum | Presse | Kontakt";


    @Test
    public void removeFirstContactPosterTemplateText() {
        String shortened = PushNotificationTextShortener.shortenText(FIRST_TEXT);

        assertEquals("Hallo, \n \nbin an den Tisch interessiert.", shortened);
    }

    @Test
    public void removeStartLine(){
        String s = "finde ich gut mehr davon \n" +
                "\n" +
                "Am 12/12/2013 01:27 PM, online aldana über eBay Kleinanzeigen schrieb:\n" +
                ">\n" +
                "> Anfrage zu Ihrer Anzeige\n" +
                ">\n" +
                "> eBay Kleinanzeigen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Logo>\n" +
                ">\n" +
                ">\n" +
                "> Lieber Nutzer!\n" +
                ">\n" +
                "> Ein Interessent hat eine Anfrage zu Ihrer Anzeige gesendet:\n" +
                "> 'Regalbrett 30cm x 80cm'\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/s-anzeige/regalbrett-30cm-x-80cm/137409380-87-3352?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=ViewAd>\n" +
                ">\n" +
                "> Anzeigennummer: 137409380\n" +
                ">\n";

        String shortened = PushNotificationTextShortener.shortenText(s);

        assertEquals("finde ich gut mehr davon", shortened);
    }
    
    @Test
    public void removeIntroUeberKleinanzeigen(){
        String s = "finde ich gut mehr davon \n" +
                "\n" +
                "On 12/12/2013 01:27 PM, online aldana über eBay Kleinanzeigen wrote:\n" +
                ">\n" +
                "> Anfrage zu Ihrer Anzeige\n" +
                ">\n" +
                "> eBay Kleinanzeigen\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Logo>\n" +
                ">\n" +
                ">\n" +
                "> Lieber Nutzer!\n" +
                ">\n" +
                "> Ein Interessent hat eine Anfrage zu Ihrer Anzeige gesendet:\n" +
                "> 'Regalbrett 30cm x 80cm'\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/s-anzeige/regalbrett-30cm-x-80cm/137409380-87-3352?utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=ViewAd>\n" +
                ">\n" +
                "> Anzeigennummer: 137409380\n" +
                ">\n" +
                "> *Nachricht von:* online-aldana\n" +
                ">\n" +
                "> interesse daran\n" +
                ">\n" +
                ">\n" +
                "> Artikel bereits verkauft? Löschen Sie Ihre Anzeige und informieren alle\n" +
                "> Interessenten, dass der Artikel bereits verkauft ist.\n" +
                ">\n" +
                "> * Schon verkauft? Anzeige löschen *\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/m-anzeige-loeschen.html?adId=137409380&grt_path=ClickSysMailContactPoster.DeleteAdStart&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=DeleteAd>\n" +
                "> * Anzeige deaktivieren *\n" +
                "> <http://kleinanzeigen.ebay.de/anzeigen/m-anzeige-pausieren.html?adId=137409380&grt_path=ClickSysMailContactPoster.DeactivateAd&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=DeactivateAd>\n" +
                ">\n" +
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
                "> <http://kleinanzeigen.ebay.de/anzeigen/spam-email-melden.html?raspd=a984abe290b1cf0d9144ea85cb3be3a6e4b17f7c58932465dcdfc76ec748a262a3f51d5bffd27c92a7635742ac7a7f1ddda32c098102b392f5dc6d7d913621aca833f420fa736ef7&utm_source=email&utm_medium=system_email&utm_campaign=email-ContactPoster&utm_content=Other>.\n" +
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

        String shortened = PushNotificationTextShortener.shortenText(s);

        assertEquals("finde ich gut mehr davon", shortened);
    }

    @Test
    public void prefixesSeveral(){
     String s="Ich bin arbeitslos und es ist kein Problem mit den vermittlungsgutschein\n" +
             "\n" +
             "Christian Hups\n" +
             "\n" +
             "123123123\n" +
             "\n" +
             " Von: anbieter-2fu4ysaodv0r4@mail.ebay-kleinanzeigen.de\n" +
             "Gesendet: ‎13‎. ‎Dezember‎ ‎2013 ‎12‎:‎45\n" +
             "An: cschp@hotmail.de\n" +
             "Betreff: Ihre Anfrage zu unserer Anzeige \"Spüler m/w auch ohne Vorkenntnisse gesucht\"\n" +
             "\n" +
             "\n" +
             "\n" +
             "Sehr geehrter Bewerber,\n" +
             "\n" +
             "\n" +
             "\n" +
             "zunächst einmal brauche ich Ihren Namen und Ihre Telefonnummer, damit ich Sie in den Terminkalender eintragen kann.\n" +
             "\n" +
             "\n" +
             "\n" +
             "Außerdem müssen wir klären, ob Sie einen Vermittlungsgutschein haben oder arbeitssuchend gemeldet sind.\n" +
             "\n" +
             "\n" +
             "Mit freundlichen Grüßen\n" +
             "Karriere24 AV UG\n" +
             "Yasmin ab\n" +
             "Sekretariat\n" +
             "\n" +
             "Großbeerenstrasse 2-10\n" +
             "12107 Berlin\n" +
             "\n" +
             "Telefon: 030 - 123 98 366\n" +
             "Fax: 030 - 123 98 367\n" +
             "\n" +
             "abboud@karriere24av.de\n" +
             "\n" +
             "\n" +
             "\n" +
             "Hi über eBay Kleinanzeigen <interessent-2pvu74hi1crsd@mail.ebay-kleinanzeigen.de> hat am 13. Dezember 2013 um 12:27 geschrieben:\n" +
             "\n" +
             "\n" +
             "\n" +
             "\n" +
             "Anfrage zu Ihrer Anzeige\n" +
             "eBay Kleinanzeigen\n"      ;

        String shortened = PushNotificationTextShortener.shortenText(s);

        assertEquals("Ich bin arbeitslos und es ist kein Problem mit den vermittlungsgutschein\n" +
                "\n" +
                "Christian Hups\n" +
                "\n" +
                "123123123", shortened);
    }

    @Test
    public void removeFirstContactPosterTemplateText2() {
        String shortened = PushNotificationTextShortener.shortenText(LATER_ANSWERS_TEXT_2);

        assertEquals("hallo, \n" +
                "\n" +
                "ist die Halterung auch mit dabei? Ist aus den Bildern und Text nicht ersichtlich.\n" +
                "\n" +
                "Danke für die Info.", shortened);
    }


    @Test
    public void removeQuotePartsOfMessage() {
        String shortened = PushNotificationTextShortener.shortenText(LATER_ANSWERS_TEXT);

        assertEquals("Ist noch da :) Abholung heute abend ab 20 uhr (Gosse.16, fhain)?", shortened);
    }

    @Test
    public void stripsHagglingFromContactPoster() {
        Assert.assertEquals("Hello", PushNotificationTextShortener.shortenText(MESSAGEBOX_HAGGLING_CP_TEXT));
    }

    @Test
    public void stripsHagglingFromReply() {
        Assert.assertEquals("und noch ein angebot!", PushNotificationTextShortener.shortenText(MESSAGEBOX_HAGGLING_REPLY_TEXT));
    }
}
